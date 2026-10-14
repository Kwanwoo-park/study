package spring.study.admin.dto;

import java.time.LocalDateTime;

public record IntegrationEventLogDiagnostics(boolean enabled, String instanceId, int pending, long dropped, long storageFailures, LocalDateTime lastSavedAt) {}
