package spring.study.admin.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import spring.study.admin.entity.SystemIncident;
import spring.study.admin.service.SystemIncidentService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(SystemIncidentService.class)
class SystemIncidentRepositoryTest {
    @Autowired SystemIncidentRepository repository;
    @Autowired SystemIncidentService service;
    @Autowired EntityManager entityManager;

    @Test
    void listsThirtyNewestPendingIncidentsAndAcknowledgesAllWithoutDeletingHistory() {
        LocalDateTime first = LocalDateTime.of(2026, 11, 17, 9, 0);
        for (int index = 0; index < 32; index++) {
            repository.save(incident(first.plusSeconds(index)));
        }
        SystemIncident acknowledged = incident(first.plusMinutes(1));
        acknowledged.acknowledge(first.plusMinutes(2));
        repository.save(acknowledged);
        repository.flush();
        entityManager.clear();

        List<SystemIncident> recent = service.findRecentUnacknowledged();
        assertThat(recent).hasSize(30).noneMatch(SystemIncident::isAcknowledged);
        assertThat(recent).extracting(SystemIncident::getOccurredAt)
                .isSortedAccordingTo(Comparator.reverseOrder()).doesNotContain(first, first.plusSeconds(1));
        assertThat(service.countUnacknowledged()).isEqualTo(32);

        Long newestId = recent.get(0).getId();
        service.acknowledge(newestId);
        repository.flush();
        entityManager.clear();
        assertThat(service.findRecentUnacknowledged()).extracting(SystemIncident::getId).doesNotContain(newestId);
        assertThat(service.countUnacknowledged()).isEqualTo(31);

        assertThat(service.acknowledgeAll()).isEqualTo(31);
        assertThat(service.findRecentUnacknowledged()).isEmpty();
        assertThat(service.countUnacknowledged()).isZero();
        assertThat(repository.findAll()).hasSize(33).allMatch(SystemIncident::isAcknowledged);
    }

    private SystemIncident incident(LocalDateTime time) {
        return SystemIncident.builder().occurredAt(time).requestMethod("GET").requestPath("/test")
                .httpStatus(500).exceptionType("TestException").errorMessage("test incident").build();
    }
}
