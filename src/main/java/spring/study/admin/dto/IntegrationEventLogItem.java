package spring.study.admin.dto;

import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.entity.IntegrationEventLog.*;
import java.time.LocalDateTime;

public record IntegrationEventLogItem(Long id, LocalDateTime occurredAt, String instanceId, Broker broker, String destination,
                   Route route, Operation operation, Outcome outcome, Long referenceId, int itemCount,
                   Integer attempt, Long subscriberCount, String errorType) {
    public IntegrationEventLogItem(IntegrationEventLog log) {
        this(
                log.getId(),
                log.getOccurredAt(),
                log.getInstanceId(),
                log.getBroker(),
                log.getRoute().destination,
                log.getRoute(),
                log.getOperation(),
                log.getOutcome(),
                log.getReferenceId(),
                log.getItemCount(),
                log.getAttempt(),
                log.getSubscriberCount(),
                log.getErrorType());
    }
}
