package spring.study.kafka.dto;

import java.time.LocalDateTime;

public record OutboxCreatedAtProjection(LocalDateTime createdAt) {}
