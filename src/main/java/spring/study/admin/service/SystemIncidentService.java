package spring.study.admin.service;

import spring.study.admin.dto.IncidentRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.dto.SystemIncidentResponseDto;
import spring.study.admin.entity.SystemIncident;
import spring.study.admin.repository.SystemIncidentRepository;
import spring.study.common.service.IpLocationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemIncidentService {
    private final SystemIncidentRepository systemIncidentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(IncidentRequestContext request, Exception exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        LocalDateTime occurredAt = LocalDateTime.now();
        String requestMethod = limit(valueOrDefault(request.method(), "UNKNOWN"), 10);
        String requestPath = limit(valueOrDefault(request.path(), "UNKNOWN"), 500);
        String requestIp = request.ipAddress();
        String exceptionType = limit(cause.getClass().getName(), 255);
        String errorMessage = limit(sanitize(valueOrDefault(cause.getMessage(), "메시지 없는 서버 오류")), 1000);
        SystemIncident existing = systemIncidentRepository.findFirstByRequestMethodAndRequestPathAndRequestIpAndExceptionTypeAndErrorMessageAndAcknowledgedFalse(requestMethod, requestPath, requestIp, exceptionType, errorMessage).orElse(null);

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
    public List<SystemIncident> findRecent() {
        return systemIncidentRepository.findTop50ByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public long countUnacknowledged() {
        return systemIncidentRepository.countByAcknowledgedFalse();
    }

    @Transactional
    public void acknowledge(Long id) {
        SystemIncident incident = systemIncidentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장애 기록입니다"));

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
