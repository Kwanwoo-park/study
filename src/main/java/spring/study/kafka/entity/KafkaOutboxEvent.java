package spring.study.kafka.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "kafka_outbox_event", indexes = @Index(name = "idx_kafka_outbox_dispatch", columnList = "dead_lettered,next_attempt_at,id"))
@NoArgsConstructor
public class KafkaOutboxEvent {
    private static final int MAX_ATTEMPT_COUNT = 10;

    public enum PayloadType { CHAT_MESSAGE, NOTIFICATION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "event_key", length = 255)
    private String eventKey;

    @Column(name = "payload_type", nullable = false, length = 30)
    private String payloadType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "dead_lettered", nullable = false)
    private boolean deadLettered;

    public KafkaOutboxEvent(String topic, String eventKey, PayloadType payloadType, String payload) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.payloadType = payloadType.name();
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.nextAttemptAt = createdAt;
    }

    public PayloadType resolvedPayloadType() {
        return PayloadType.valueOf(payloadType);
    }

    public void recordFailure(String error, LocalDateTime retryAt) {
        attemptCount++;
        lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 1000));
        deadLettered = attemptCount >= MAX_ATTEMPT_COUNT;
        nextAttemptAt = deadLettered ? null : retryAt;
    }
}
