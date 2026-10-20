package spring.study.admin.entity;

import spring.study.admin.dto.IntegrationEventLogEntry;

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "instance_id", nullable = false, length = 36)
    private String instanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IntegrationEventLogBroker broker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IntegrationEventLogRoute route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationEventLogOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IntegrationEventLogOutcome outcome;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    private Integer attempt;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "error_type")
    private String errorType;

    public IntegrationEventLog(IntegrationEventLogEntry entry) {
        occurredAt = entry.occurredAt();
        instanceId = entry.instanceId();
        route = entry.route();
        broker = route.broker;
        operation = entry.operation();
        outcome = entry.outcome();
        referenceId = entry.referenceId();
        itemCount = entry.itemCount();
        attempt = entry.attempt();
        subscriberCount = entry.subscriberCount();
        errorType = entry.errorType();
    }
}
