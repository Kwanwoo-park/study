package spring.study.kafka.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.entity.KafkaOutboxPayloadType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class KafkaOutboxEventRepositoryTest {
    @Autowired
    private KafkaOutboxEventRepository repository;

    @Test
    void queryShouldReturnOnlyEventsWhoseRetryTimeHasArrived() {
        KafkaOutboxEvent event = repository.saveAndFlush(new KafkaOutboxEvent("topic", "room-1", KafkaOutboxPayloadType.CHAT_MESSAGE, "{}"));
        LocalDateTime now = LocalDateTime.now();

        List<KafkaOutboxEvent> readyEvents = repository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(now.plusSeconds(1), PageRequest.of(0, 100));
        assertEquals(1, readyEvents.size());

        event.recordFailure("Kafka unavailable", now.plusMinutes(10));
        repository.flush();

        List<KafkaOutboxEvent> delayedEvents = repository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(now.plusMinutes(1), PageRequest.of(0, 100));
        assertTrue(delayedEvents.isEmpty());
    }

    @Test
    void queryExcludesDeadLettersFromBothRetryBranchesAndLimitsTheBatch() {
        LocalDateTime now = LocalDateTime.now();
        KafkaOutboxEvent immediate = event(now.minusMinutes(3), null, false);
        KafkaOutboxEvent retry = event(now.minusMinutes(2), now, false);
        event(now.minusMinutes(4), null, true);
        event(now.minusMinutes(5), now.minusSeconds(1), true);
        event(now.minusMinutes(1), now.plusMinutes(1), false);
        repository.flush();

        assertThat(repository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(
                now, PageRequest.of(0, 100))).extracting(KafkaOutboxEvent::getId)
                .containsExactly(immediate.getId(), retry.getId());
        assertThat(repository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(
                now, PageRequest.of(0, 1))).extracting(KafkaOutboxEvent::getId).containsExactly(immediate.getId());
        assertThat(repository.findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc())
                .get().extracting(spring.study.kafka.dto.OutboxCreatedAtProjection::createdAt)
                .isEqualTo(immediate.getCreatedAt());
    }

    @Test
    void oldestPendingProjectionIsEmptyWhenOnlyDeadLettersRemain() {
        event(LocalDateTime.now(), null, true);
        repository.flush();

        assertThat(repository.findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc()).isEmpty();
    }

    private KafkaOutboxEvent event(LocalDateTime createdAt, LocalDateTime nextAttemptAt, boolean deadLettered) {
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room-1", KafkaOutboxPayloadType.CHAT_MESSAGE, "{}");
        ReflectionTestUtils.setField(event, "createdAt", createdAt);
        ReflectionTestUtils.setField(event, "nextAttemptAt", nextAttemptAt);
        ReflectionTestUtils.setField(event, "deadLettered", deadLettered);
        return repository.save(event);
    }
}
