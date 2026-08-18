package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.admin.service.SystemIncidentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedExceptionShouldBeRecordedAndReturnInternalServerError() {
        SystemIncidentService incidentService = mock(SystemIncidentService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RuntimeException exception = new RuntimeException("failure");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleEtc(exception, request);

        verify(incidentService).record(request, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void recordingFailureShouldNotReplaceOriginalErrorResponse() {
        SystemIncidentService incidentService = mock(SystemIncidentService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RuntimeException exception = new RuntimeException("failure");
        doThrow(new RuntimeException("recording failed")).when(incidentService).record(request, exception);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleEtc(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void illegalStateShouldBeRecordedInsteadOfBeingReportedAsUnauthorized() {
        SystemIncidentService incidentService = mock(SystemIncidentService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        IllegalStateException exception = new IllegalStateException("redis unavailable");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleIllegalState(exception, request);

        verify(incidentService).record(request, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
