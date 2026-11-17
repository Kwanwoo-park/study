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
    void onlyUnacknowledgedIncidentsAreListedAndAcknowledgingPreservesHistory() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 17, 9, 0);
        SystemIncident older = repository.save(incident(now.minusHours(2)));
        SystemIncident newer = repository.save(incident(now.minusHours(1)));
        for (int index = 0; index < 50; index++) {
            SystemIncident acknowledged = incident(now.plusSeconds(index));
            acknowledged.acknowledge(now.plusMinutes(1));
            repository.save(acknowledged);
        }
        repository.flush();
        entityManager.clear();

        assertThat(service.findRecentUnacknowledged()).extracting(SystemIncident::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(service.countUnacknowledged()).isEqualTo(2);

        service.acknowledge(newer.getId());
        repository.flush();
        entityManager.clear();
        assertThat(service.findRecentUnacknowledged()).extracting(SystemIncident::getId)
                .containsExactly(older.getId());

        assertThat(service.acknowledgeAll()).isEqualTo(1);
        assertThat(service.findRecentUnacknowledged()).isEmpty();
        assertThat(service.countUnacknowledged()).isZero();
        assertThat(repository.count()).isEqualTo(52);
        assertThat(repository.findById(older.getId()).orElseThrow().isAcknowledged()).isTrue();
        assertThat(repository.findById(newer.getId()).orElseThrow().isAcknowledged()).isTrue();
    }

    @Test
    void listsOnlyThirtyNewestIncidentsButAcknowledgesAllPendingIncidents() {
        LocalDateTime first = LocalDateTime.of(2026, 11, 17, 9, 0);
        for (int index = 0; index < 31; index++) repository.save(incident(first.plusSeconds(index)));
        repository.flush();
        entityManager.clear();

        List<SystemIncident> recent = service.findRecentUnacknowledged();
        assertThat(recent).hasSize(30);
        assertThat(recent).extracting(SystemIncident::getOccurredAt)
                .isSortedAccordingTo(Comparator.reverseOrder()).doesNotContain(first);
        assertThat(service.countUnacknowledged()).isEqualTo(31);
        assertThat(service.acknowledgeAll()).isEqualTo(31);
        assertThat(service.findRecentUnacknowledged()).isEmpty();
    }

    private SystemIncident incident(LocalDateTime time) {
        return SystemIncident.builder().occurredAt(time).requestMethod("GET").requestPath("/test")
                .httpStatus(500).exceptionType("TestException").errorMessage("test incident").build();
    }
}
