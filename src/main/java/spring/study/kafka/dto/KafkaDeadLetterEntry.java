package spring.study.kafka.dto;

import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import java.time.LocalDateTime;

public record KafkaDeadLetterEntry(Long id, String topic, int partition, long offset, KafkaDeadLetterStatus status, String errorType,
                    LocalDateTime createdAt, LocalDateTime replayedAt, LocalDateTime nextAttemptAt, int attempts, boolean replayable) {
    public static KafkaDeadLetterEntry from(KafkaDeadLetterRepository.Metadata e) {
        return new KafkaDeadLetterEntry(e.getId(), e.getSourceTopic(), e.getSourcePartition(), e.getSourceOffset(), e.getStatus(), e.getErrorType(),
                e.getCreatedAt(), e.getReplayedAt(), e.getNextAttemptAt(), e.getAttemptCount(), e.getPayloadAvailable() && (e.getStatus() == KafkaDeadLetterStatus.NEW || e.getStatus() == KafkaDeadLetterStatus.REPLAY_FAILED));
    }
}
