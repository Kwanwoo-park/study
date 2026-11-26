package spring.study.mail.component;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.component.TransactionRunner;
import spring.study.mail.repository.EmailVerificationAttemptRepository;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EmailVerificationGuard {
    private final EmailVerificationAttemptRepository attempts;
    private final TransactionRunner transactions;

    public void recordRequest(String emailDigest) {
        boolean allowed = transactions.write(() -> {
            attempts.ensureCounter(emailDigest);
            return attempts.findCounterForUpdate(emailDigest).orElseThrow().allowRequest(Instant.now());
        });

        if (!allowed) throw blockedEmail();
    }

    public boolean isBlocked(String emailDigest) {
        return attempts.existsByEmailDigestAndBlockedAtIsNotNull(emailDigest);
    }

    public void requireAllowed(String emailDigest) {
        if (isBlocked(emailDigest)) throw blockedEmail();
    }

    private ResponseStatusException blockedEmail() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "인증 요청 횟수를 초과해 이메일 인증이 영구 차단되었습니다.");
    }
}
