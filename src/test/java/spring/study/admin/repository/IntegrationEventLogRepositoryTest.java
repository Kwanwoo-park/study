package spring.study.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.admin.entity.IntegrationEventLog;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static spring.study.admin.entity.IntegrationEventLog.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class IntegrationEventLogRepositoryTest {
    @Autowired IntegrationEventLogRepository repository;

    @Test
    void supportsOptionalFiltersCursorAndTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new IntegrationEventLog(new Entry(now.minusDays(8), "node", Route.CHAT, Operation.PUBLISH, Outcome.SUCCESS, 1L, 1, 1, null, null)));
        IntegrationEventLog kafka = repository.save(new IntegrationEventLog(new Entry(now, "node", Route.CHAT, Operation.PUBLISH, Outcome.RETRY_SCHEDULED, 2L, 1, 2, null, "java.lang.IllegalStateException")));
        IntegrationEventLog redis = repository.saveAndFlush(new IntegrationEventLog(new Entry(now, "node", Route.REALTIME_NOTIFICATION, Operation.CONSUME, Outcome.SUCCESS, 3L, 1, null, null, null)));
        assertThat(repository.search(now.minusDays(7), null, null, null, null, PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(redis.getId(), kafka.getId());
        assertThat(repository.search(now.minusDays(7), Broker.KAFKA, Operation.PUBLISH, Outcome.RETRY_SCHEDULED, null, PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(kafka.getId());
        assertThat(repository.search(now.minusDays(7), null, null, null, redis.getId(), PageRequest.of(0, 51)))
                .extracting(IntegrationEventLog::getId).containsExactly(kafka.getId());
    }

    @Test
    void cleanupDeletesOnlyExpiredLogsInBoundedBatches() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new IntegrationEventLog(new Entry(now.minusDays(8), "node", Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 1, null, null, null)));
        IntegrationEventLog retained = repository.saveAndFlush(new IntegrationEventLog(new Entry(now, "node", Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 1, null, null, null)));
        List<Long> ids = repository.findExpiredIds(now.minusDays(7), PageRequest.of(0, 1000));
        assertThat(ids).hasSize(1);
        repository.deleteAllByIdInBatch(ids);
        assertThat(repository.findAll()).extracting(IntegrationEventLog::getId).containsExactly(retained.getId());
    }
}
