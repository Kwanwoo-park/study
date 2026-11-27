package spring.study.mail.component;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.component.TransactionRunner;
import spring.study.mail.entity.EmailVerificationAttempt;
import spring.study.mail.repository.EmailVerificationAttemptRepository;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailVerificationGuard {
    private final EmailVerificationAttemptRepository attempts;
    private final TransactionRunner transactions;

    public void recordRequest(String emailDigest, String emailAddress) {
        boolean allowed = transactions.write(() ->
                counterForUpdate(emailDigest).allowRequest(Instant.now(), emailAddress));

        if (!allowed) throw blockedEmail();
    }

    public void block(String emailDigest, String emailAddress) {
        transactions.write(() -> {
            counterForUpdate(emailDigest).block(Instant.now(), emailAddress);
            return null;
        });
    }

    public List<EmailVerificationAttempt> listBlocked() {
        return attempts.findAllByBlockedAtIsNotNullOrderByBlockedAtDesc();
    }

    public boolean isBlocked(String emailDigest) {
        return attempts.existsByEmailDigestAndBlockedAtIsNotNull(emailDigest);
    }

    public void requireAllowed(String emailDigest) {
        if (isBlocked(emailDigest)) throw blockedEmail();
    }

    private EmailVerificationAttempt counterForUpdate(String emailDigest) {
        attempts.ensureCounter(emailDigest);
        return attempts.findCounterForUpdate(emailDigest).orElseThrow();
    }

    private ResponseStatusException blockedEmail() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 인증이 영구 차단되었습니다.");
    }
}
