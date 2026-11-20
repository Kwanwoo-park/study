package spring.study.mail.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailFacade {
    private final EmailVerificationService verificationService;

    public ResponseEntity<Map<String, Object>> confirm(String email) {
        verificationService.send(EmailVerificationPurpose.EMAIL, email, email);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "expiresInSeconds", EmailVerificationService.CODE_TTL.toSeconds(),
                "resendAfterSeconds", EmailVerificationService.RESEND_COOLDOWN.toSeconds()
        ));
    }

    public ResponseEntity<Map<String, Object>> verify(String email, String code) {
        String token = verificationService.verify(EmailVerificationPurpose.EMAIL, email, code);

        return ResponseEntity.ok(Map.of(
                "verificationToken", token,
                "status", "ok",
                "expiresInSeconds", EmailVerificationService.CODE_TTL.toSeconds()
        ));
    }
}
