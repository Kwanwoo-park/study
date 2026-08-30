package spring.study.admin.dto;

import spring.study.admin.entity.SystemIncident;
import spring.study.common.dto.IpLocationResponse;

import java.time.LocalDateTime;

public record SystemIncidentResponseDto(Long id, LocalDateTime occurredAt, String requestMethod, String requestPath, String requestIp, IpLocationResponse requestLocation, int httpStatus, String exceptionType, String message, long occurrenceCount, boolean acknowledged, LocalDateTime acknowledgedAt) {
    public SystemIncidentResponseDto(SystemIncident incident, IpLocationResponse requestLocation) {
        this(
                incident.getId(),
                incident.getOccurredAt(),
                incident.getRequestMethod(),
                incident.getRequestPath(),
                incident.getRequestIp(),
                requestLocation,
                incident.getHttpStatus(),
                incident.getExceptionType(),
                incident.getErrorMessage(),
                incident.getOccurrenceCount(),
                incident.isAcknowledged(),
                incident.getAcknowledgedAt()
        );
    }
}
