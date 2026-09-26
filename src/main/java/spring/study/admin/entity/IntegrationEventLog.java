package spring.study.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "integration_event_log", indexes = {
        @Index(name = "idx_event_log_time", columnList = "occurred_at,id"),
        @Index(name = "idx_event_log_broker", columnList = "broker,id")
})
public class IntegrationEventLog {
    public enum Broker { KAFKA, REDIS }
    public enum Operation { QUEUE, PUBLISH, CONSUME, CONSUMER_RETRY, REPLAY }
    public enum Outcome { QUEUED, SUCCESS, FAILED, RETRY_SCHEDULED, DEAD_LETTER, DLT_PUBLISHED, DUPLICATE, SKIPPED, NO_SUBSCRIBERS, DISCARDED }
    public enum Route {
        CHAT(Broker.KAFKA, "topic"), NOTIFICATION(Broker.KAFKA, "topic2"),
        REALTIME_NOTIFICATION(Broker.REDIS, "notification-events"), UNKNOWN_KAFKA(Broker.KAFKA, "unknown");
        public final Broker broker;
        public final String destination;
        Route(Broker broker, String destination) { this.broker = broker; this.destination = destination; }
        public static Route kafka(String topic) {
            return "topic".equals(topic) ? CHAT : "topic2".equals(topic) ? NOTIFICATION : UNKNOWN_KAFKA;
        }
    }

    // Metadata only: never store payload, Kafka key, token, email, or exception message.
    public record Entry(LocalDateTime occurredAt, String instanceId, Route route, Operation operation,
                        Outcome outcome, Long referenceId, int itemCount, Integer attempt,
                        Long subscriberCount, String errorType) {}

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    @Column(name = "instance_id", nullable = false, length = 36)
    private String instanceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Broker broker;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Route route;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Operation operation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Outcome outcome;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(name = "item_count", nullable = false)
    private int itemCount;
    private Integer attempt;
    @Column(name = "subscriber_count")
    private Long subscriberCount;
    @Column(name = "error_type", length = 255)
    private String errorType;

    public IntegrationEventLog(Entry entry) {
        occurredAt = entry.occurredAt(); instanceId = entry.instanceId(); route = entry.route();
        broker = route.broker; operation = entry.operation(); outcome = entry.outcome();
        referenceId = entry.referenceId(); itemCount = entry.itemCount(); attempt = entry.attempt();
        subscriberCount = entry.subscriberCount(); errorType = entry.errorType();
    }
}
