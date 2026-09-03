package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.service.SystemIncidentService;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SystemIncidentService systemIncidentService;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "result", -10,
                        "message", e.getMessage()
                ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<?> handleMissingRequestPart(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "result", -400,
                "message", "업로드할 파일을 다시 선택하여 주십시오"
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "result", -413,
                "message", "업로드 가능한 파일 용량을 초과하였습니다"
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e, HttpServletRequest request) {
        return handleEtc(e, request);
    }

    @ExceptionHandler(BusinessStateException.class)
    public ResponseEntity<?> handleBusinessState(BusinessStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "result", -409,
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "result", -404,
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.debug("클라이언트가 연결을 종료했습니다: {}", e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e) {
        log.debug("연결이 종료되었습니다: {}", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("result", -403));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of(
                        "result", -e.getStatusCode().value(),
                        "message", e.getReason() == null ? "요청을 처리할 수 없습니다" : e.getReason()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e, HttpServletRequest request) {
        log.error("서버 오류", e);
        try {
            systemIncidentService.record(request, e);
        } catch (Exception recordException) {
            log.error("장애 기록 저장 실패", recordException);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("result", -500));
    }
}
