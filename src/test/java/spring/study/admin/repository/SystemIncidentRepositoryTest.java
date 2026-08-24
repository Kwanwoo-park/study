package spring.study.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import spring.study.admin.entity.SystemIncident;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY,
        connection = EmbeddedDatabaseConnection.H2
)
class SystemIncidentRepositoryTest {

    @Autowired
    private SystemIncidentRepository systemIncidentRepository;

    @Test
    void acknowledgeAllShouldUpdateOnlyUnacknowledgedIncidents() {
        LocalDateTime previouslyAcknowledgedAt = LocalDateTime.of(2026, 8, 23, 12, 0);
        SystemIncident previouslyAcknowledged = incident("previously acknowledged");
        previouslyAcknowledged.acknowledge(previouslyAcknowledgedAt);

        systemIncidentRepository.save(incident("first failure"));
        systemIncidentRepository.save(incident("second failure"));
        SystemIncident savedAcknowledged = systemIncidentRepository.save(previouslyAcknowledged);
        LocalDateTime acknowledgedAt = LocalDateTime.of(2026, 8, 24, 10, 30);

        int updatedCount = systemIncidentRepository.acknowledgeAll(acknowledgedAt);

        assertThat(updatedCount).isEqualTo(2);
        assertThat(systemIncidentRepository.countByAcknowledgedFalse()).isZero();
        assertThat(systemIncidentRepository.findAll())
                .allMatch(SystemIncident::isAcknowledged);
        assertThat(systemIncidentRepository.findById(savedAcknowledged.getId()).orElseThrow().getAcknowledgedAt())
                .isEqualTo(previouslyAcknowledgedAt);
    }

    private SystemIncident incident(String message) {
        return SystemIncident.builder()
                .occurredAt(LocalDateTime.now())
                .requestMethod("GET")
                .requestPath("/test")
                .requestIp("127.0.0.1")
                .httpStatus(500)
                .exceptionType(RuntimeException.class.getName())
                .errorMessage(message)
                .build();
    }
}
