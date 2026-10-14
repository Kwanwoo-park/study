package spring.study.admin.dto;

import spring.study.admin.entity.IntegrationEventLog.Route;
import spring.study.admin.entity.IntegrationEventLog.Operation;
import spring.study.admin.entity.IntegrationEventLog.Outcome;
import java.time.LocalDateTime;

public record IntegrationEventLogEntry(LocalDateTime occurredAt, String instanceId, Route route, Operation operation,
                    Outcome outcome, Long referenceId, int itemCount, Integer attempt,
                    Long subscriberCount, String errorType) {}
