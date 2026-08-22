package spring.study.admin.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.study.admin.entity.SystemIncident;
import spring.study.admin.repository.SystemIncidentRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemIncidentServiceTest {

    @Mock
    private SystemIncidentRepository systemIncidentRepository;

    @Mock
    private HttpServletRequest request;

    @Test
    void recordShouldPersistSanitizedMostSpecificExceptionDetails() {
        SystemIncidentService service = new SystemIncidentService(systemIncidentRepository);
        RuntimeException exception = new RuntimeException("outer", new IllegalStateException("database\nfailed\tbadly"));
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/member/detail");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 10.0.0.10");

        service.record(request, exception);

        ArgumentCaptor<SystemIncident> captor = ArgumentCaptor.forClass(SystemIncident.class);
        verify(systemIncidentRepository).save(captor.capture());
        SystemIncident incident = captor.getValue();
        assertEquals("GET", incident.getRequestMethod());
        assertEquals("/api/member/detail", incident.getRequestPath());
        assertEquals("203.0.113.7", incident.getRequestIp());
        assertEquals(500, incident.getHttpStatus());
        assertEquals(IllegalStateException.class.getName(), incident.getExceptionType());
        assertEquals("database failed badly", incident.getErrorMessage());
        assertNotNull(incident.getOccurredAt());
    }

    @Test
    void acknowledgeShouldMarkIncidentAsAcknowledged() {
        SystemIncidentService service = new SystemIncidentService(systemIncidentRepository);
        SystemIncident incident = SystemIncident.builder()
                .occurredAt(LocalDateTime.now())
                .requestMethod("GET")
                .requestPath("/test")
                .requestIp("127.0.0.1")
                .httpStatus(500)
                .exceptionType(RuntimeException.class.getName())
                .errorMessage("failure")
                .build();
        when(systemIncidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        service.acknowledge(1L);

        assertTrue(incident.isAcknowledged());
        assertNotNull(incident.getAcknowledgedAt());
    }
}
