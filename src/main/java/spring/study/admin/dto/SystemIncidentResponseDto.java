package spring.study.admin.dto;

import spring.study.admin.entity.SystemIncident;

import java.time.LocalDateTime;

public record SystemIncidentResponseDto(Long id, LocalDateTime occurredAt, String requestMethod, String requestPath, String requestIp, int httpStatus, String exceptionType, String message, long occurrenceCount, boolean acknowledged, LocalDateTime acknowledgedAt) {
    public SystemIncidentResponseDto(SystemIncident incident) {
        this(
                incident.getId(),
                incident.getOccurredAt(),
                incident.getRequestMethod(),
                incident.getRequestPath(),
                incident.getRequestIp(),
                incident.getHttpStatus(),
                incident.getExceptionType(),
                incident.getErrorMessage(),
                incident.getOccurrenceCount(),
                incident.isAcknowledged(),
                incident.getAcknowledgedAt()
        );
    }
}
