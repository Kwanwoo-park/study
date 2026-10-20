package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.facade.SystemIncidentFacade;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;

import java.io.IOException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final SystemIncidentFacade systemIncidentFacade;

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
        return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "result", -e.getStatusCode().value(),
                        "message", e.getReason() == null ? "요청을 처리할 수 없습니다" : e.getReason()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e, HttpServletRequest request) {
        log.error("서버 오류", e);
        try {
            systemIncidentFacade.record(request, e);
        } catch (Exception recordException) {
            log.error("장애 기록 저장 실패", recordException);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("result", -500));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidFilter() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("result", -400, "message", "조회 조건을 확인해 주세요"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> databaseError(DataAccessException error, HttpServletRequest request) {
        handleEtc(error, request);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
                .body(Map.of("result", -503, "message", "데이터를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }
}
