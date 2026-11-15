package spring.study.mail.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class MailFacade {
    private final EmailVerificationService verificationService;

    public ResponseEntity<HashMap<String, String>> confirm(String email) {
        verificationService.send(EmailVerificationPurpose.EMAIL, email, email);

        HashMap<String, String> result = new HashMap<>();

        result.put("status", "ok");

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<HashMap<String, String>> verify(String email, String code) {
        String token = verificationService.verify(EmailVerificationPurpose.EMAIL, email, code);

        HashMap<String, String> result = new HashMap<>();

        result.put("verificationToken", token);
        result.put("status", "ok");

        return ResponseEntity.ok(result);
    }
}
