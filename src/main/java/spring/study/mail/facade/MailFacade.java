package spring.study.mail.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.mail.service.EmailVerificationService;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class MailFacade {
    private final EmailVerificationService verificationService;

    public ResponseEntity<HashMap<String, String>> confirm(String email) throws Exception {
        HashMap<String, String> result = new HashMap<>();

        verificationService.send(email);
        result.put("status", "ok");

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<HashMap<String, String>> verify(String email, String code) {
        HashMap<String, String> result = new HashMap<>();
        result.put("verificationToken", verificationService.verify(email, code));
        result.put("status", "ok");
        return ResponseEntity.ok(result);
    }
}
