package spring.study.common.facade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.Map;

@Service
public class CommonFacade {

    public ResponseEntity<?> validationFailure(BindingResult bindingResult, String fallbackMessage) {
        String message = bindingResult.getFieldErrors().isEmpty()
                ? fallbackMessage : bindingResult.getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.badRequest().body(Map.of(
                "result", -10L,
                "message", message == null ? fallbackMessage : message
        ));
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((T) Map.of(
                "result", -10L,
                "message", "유효하지 않은 세션"
        ));
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> wrongAccess() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((T) Map.of(
                "result", -10L,
                "message", "잘못된 접근입니다"
        ));
    }

    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> abnormalAccess() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body((T) Map.of(
                "result", -10L,
                "message", "비정상적인 접근입니다"
        ));
    }
}
