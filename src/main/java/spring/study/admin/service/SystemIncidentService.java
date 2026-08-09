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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemIncidentService {
    private static final int MAX_PATH_LENGTH = 500;
    private static final int MAX_TYPE_LENGTH = 255;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final SystemIncidentRepository systemIncidentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, Exception exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        systemIncidentRepository.save(SystemIncident.builder()
                .occurredAt(LocalDateTime.now())
                .requestMethod(limit(valueOrDefault(request.getMethod(), "UNKNOWN"), 10))
                .requestPath(limit(valueOrDefault(request.getRequestURI(), "UNKNOWN"), MAX_PATH_LENGTH))
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .exceptionType(limit(cause.getClass().getName(), MAX_TYPE_LENGTH))
                .errorMessage(limit(sanitize(valueOrDefault(cause.getMessage(), "메시지 없는 서버 오류")), MAX_MESSAGE_LENGTH))
                .build());
    }

    @Transactional(readOnly = true)
    public List<SystemIncidentResponseDto> findRecent() {
        return systemIncidentRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(SystemIncidentResponseDto::new)
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
