package spring.study.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.dto.SystemIncidentResponseDto;
import spring.study.admin.entity.SystemIncident;
import spring.study.admin.repository.SystemIncidentRepository;
import spring.study.common.service.ClientIpResolver;
import spring.study.common.service.IpLocationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemIncidentService {
    private static final int MAX_PATH_LENGTH = 500;
    private static final int MAX_TYPE_LENGTH = 255;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final SystemIncidentRepository systemIncidentRepository;
    private final IpLocationService ipLocationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, Exception exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        LocalDateTime occurredAt = LocalDateTime.now();
        String requestMethod = limit(valueOrDefault(request.getMethod(), "UNKNOWN"), 10);
        String requestPath = limit(valueOrDefault(request.getRequestURI(), "UNKNOWN"), MAX_PATH_LENGTH);
        String requestIp = ClientIpResolver.resolve(request);
        String exceptionType = limit(cause.getClass().getName(), MAX_TYPE_LENGTH);
        String errorMessage = limit(sanitize(valueOrDefault(cause.getMessage(), "메시지 없는 서버 오류")), MAX_MESSAGE_LENGTH);

        SystemIncident existing = systemIncidentRepository
                .findFirstByRequestMethodAndRequestPathAndRequestIpAndExceptionTypeAndErrorMessageAndAcknowledgedFalse(requestMethod, requestPath, requestIp, exceptionType, errorMessage)
                .orElse(null);
        if (existing != null) {
            existing.recordRecurrence(occurredAt);
            return;
        }

        systemIncidentRepository.save(SystemIncident.builder()
                .occurredAt(occurredAt)
                .requestMethod(requestMethod)
                .requestPath(requestPath)
                .requestIp(requestIp)
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .exceptionType(exceptionType)
                .errorMessage(errorMessage)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SystemIncidentResponseDto> findRecent() {
        return systemIncidentRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(incident -> new SystemIncidentResponseDto(
                        incident,
                        ipLocationService.find(incident.getRequestIp())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnacknowledged() {
        return systemIncidentRepository.countByAcknowledgedFalse();
    }

    @Transactional
    public void acknowledge(Long id) {
        SystemIncident incident = systemIncidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장애 기록입니다"));
        if (!incident.isAcknowledged()) {
            incident.acknowledge(LocalDateTime.now());
        }
    }

    @Transactional
    public int acknowledgeAll() {
        return systemIncidentRepository.acknowledgeAll(LocalDateTime.now());
    }

    private String sanitize(String value) {
        return value.replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
