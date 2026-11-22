package spring.study.kafka.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KafkaOutboxFailure(Long id, String topic, LocalDateTime createdAt, int attemptCount) {
    public record Listing(List<KafkaOutboxFailure> entries, Long nextBeforeId) {}
}
