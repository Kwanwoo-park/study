package spring.study.admin.dto;

import java.util.List;

public record IntegrationEventLogResponse(List<IntegrationEventLogItem> entries, Long nextBeforeId, int retentionDays, IntegrationEventLogDiagnostics diagnostics) {
}
