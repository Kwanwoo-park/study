package spring.study.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.dto.IntegrationEventLogEntry;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import spring.study.admin.entity.IntegrationEventLogBroker;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class IntegrationEventLogRepositoryTest {
    @Autowired IntegrationEventLogRepository repository;

    @Test
    void supportsOptionalFiltersCursorAndTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new IntegrationEventLog(new IntegrationEventLogEntry(now.minusDays(8), "node", IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.SUCCESS, 1L, 1, 1, null, null)));
        IntegrationEventLog kafka = repository.save(new IntegrationEventLog(new IntegrationEventLogEntry(now, "node", IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.RETRY_SCHEDULED, 2L, 1, 2, null, "java.lang.IllegalStateException")));
        IntegrationEventLog redis = repository.saveAndFlush(new IntegrationEventLog(new IntegrationEventLogEntry(now, "node", IntegrationEventLogRoute.REALTIME_NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, 3L, 1, null, null, null)));
        assertThat(repository.search(now.minusDays(7), null, null, null, null, PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(redis.getId(), kafka.getId());
        assertThat(repository.search(now.minusDays(7), IntegrationEventLogBroker.KAFKA, IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.RETRY_SCHEDULED, null, PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(kafka.getId());
        assertThat(repository.search(now.minusDays(7), null, null, null, redis.getId(), PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(kafka.getId());
    }

    @Test
    void cleanupDeletesOnlyExpiredLogsInBoundedBatches() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new IntegrationEventLog(new IntegrationEventLogEntry(now.minusDays(8), "node", IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, null, 1, null, null, null)));
        IntegrationEventLog retained = repository.saveAndFlush(new IntegrationEventLog(new IntegrationEventLogEntry(now, "node", IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, null, 1, null, null, null)));
        List<Long> ids = repository.findExpiredIds(now.minusDays(7), PageRequest.of(0, 1000));
        assertThat(ids).hasSize(1);
        repository.deleteAllByIdInBatch(ids);
        assertThat(repository.findAll()).extracting(IntegrationEventLog::getId).containsExactly(retained.getId());
    }
}
