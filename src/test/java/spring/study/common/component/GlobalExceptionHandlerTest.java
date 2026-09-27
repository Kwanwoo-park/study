package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import spring.study.admin.facade.SystemIncidentFacade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalExceptionHandlerTest {

    @Test
    void missingUploadPartShouldReturnBadRequestWithoutRecordingAnIncident() {
        SystemIncidentFacade incidentService = mock(SystemIncidentFacade.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleMissingRequestPart(
                new MissingServletRequestPartException("file"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(incidentService);
    }

    @Test
    void oversizedUploadShouldReturnPayloadTooLargeWithoutRecordingAnIncident() {
        SystemIncidentFacade incidentService = mock(SystemIncidentFacade.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(10L));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        verifyNoInteractions(incidentService);
    }

    @Test
    void unexpectedExceptionShouldBeRecordedAndReturnInternalServerError() {
        SystemIncidentFacade incidentService = mock(SystemIncidentFacade.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RuntimeException exception = new RuntimeException("failure");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleEtc(exception, request);

        verify(incidentService).record(request, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void recordingFailureShouldNotReplaceOriginalErrorResponse() {
        SystemIncidentFacade incidentService = mock(SystemIncidentFacade.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RuntimeException exception = new RuntimeException("failure");
        doThrow(new RuntimeException("recording failed")).when(incidentService).record(request, exception);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleEtc(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void illegalStateShouldBeRecordedInsteadOfBeingReportedAsUnauthorized() {
        SystemIncidentFacade incidentService = mock(SystemIncidentFacade.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        IllegalStateException exception = new IllegalStateException("redis unavailable");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(incidentService);

        ResponseEntity<?> response = handler.handleIllegalState(exception, request);

        verify(incidentService).record(request, exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
