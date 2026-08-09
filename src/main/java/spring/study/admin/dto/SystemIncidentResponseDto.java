package spring.study.admin.dto;

import spring.study.admin.entity.SystemIncident;

import java.time.LocalDateTime;

public record SystemIncidentResponseDto(
        Long id,
        LocalDateTime occurredAt,
        String requestMethod,
        String requestPath,
        int httpStatus,
        String exceptionType,
        String message,
        boolean acknowledged,
        LocalDateTime acknowledgedAt
) {
    public SystemIncidentResponseDto(SystemIncident incident) {
        this(
                incident.getId(),
                incident.getOccurredAt(),
                incident.getRequestMethod(),
                incident.getRequestPath(),
                incident.getHttpStatus(),
                incident.getExceptionType(),
                incident.getErrorMessage(),
                incident.isAcknowledged(),
                incident.getAcknowledgedAt()
        );
    }
}
