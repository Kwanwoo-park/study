package spring.study.admin.dto;

import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import java.time.LocalDateTime;

public record IntegrationEventLogEntry(LocalDateTime occurredAt, String instanceId, IntegrationEventLogRoute route, IntegrationEventLogOperation operation,
                    IntegrationEventLogOutcome outcome, Long referenceId, int itemCount, Integer attempt,
                    Long subscriberCount, String errorType) {}
