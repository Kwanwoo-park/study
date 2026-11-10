package spring.study.kafka.dto;

import spring.study.kafka.entity.KafkaDeadLetterStatus;
import java.time.LocalDateTime;

public record KafkaDeadLetterEntry(Long id, String topic, int partition, long offset, KafkaDeadLetterStatus status, String errorType,
                    LocalDateTime createdAt, LocalDateTime replayedAt, LocalDateTime nextAttemptAt, int attempts, boolean replayable) {

    public static KafkaDeadLetterEntry from(KafkaDeadLetterMetadataDto e) {
        return new KafkaDeadLetterEntry(e.id(), e.sourceTopic(), e.sourcePartition(), e.sourceOffset(), e.status(), e.errorType(),
                e.createdAt(), e.replayedAt(), e.nextAttemptAt(), e.attemptCount(), e.payloadAvailable() && (e.status() == KafkaDeadLetterStatus.NEW || e.status() == KafkaDeadLetterStatus.REPLAY_FAILED));
    }
}
