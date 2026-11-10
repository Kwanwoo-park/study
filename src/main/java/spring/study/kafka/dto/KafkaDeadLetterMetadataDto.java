package spring.study.kafka.dto;

import spring.study.kafka.entity.KafkaDeadLetterStatus;

import java.time.LocalDateTime;

public record KafkaDeadLetterMetadataDto(
        Long id,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        KafkaDeadLetterStatus status,
        String errorType,
        LocalDateTime createdAt,
        LocalDateTime replayedAt,
        LocalDateTime nextAttemptAt,
        int attemptCount,
        boolean payloadAvailable
) {}
