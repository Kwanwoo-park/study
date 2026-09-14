package spring.study.mail.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.mail.service.RegisterMail;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class MailFacade {
    private final RegisterMail registerMail;

    public ResponseEntity<HashMap<String, String>> confirm(String email) {
        HashMap<String, String> result = new HashMap<>();

        try {
            String code = registerMail.sendSimpleMessage(email);
            result.put("status", "ok");
            result.put("code", code);
        } catch (Exception e) {
            result.put("status", "fail");
        }

        return ResponseEntity.ok(result);
    }
}
