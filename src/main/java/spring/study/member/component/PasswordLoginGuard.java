package spring.study.member.component;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.service.IpBlockService;
import spring.study.common.component.TransactionRunner;
import spring.study.common.service.ClientIpResolver;
import spring.study.member.entity.Member;
import spring.study.member.repository.LoginIpAttemptRepository;
import spring.study.member.service.MemberService;

@Component
@RequiredArgsConstructor
public class PasswordLoginGuard {
    private static final int ALLOWED_FAILURES = 5;

    private final LoginIpAttemptRepository attempts;
    private final MemberService members;
    private final BCryptPasswordEncoder encoder;
    private final IpBlockService blocks;
    private final TransactionRunner transactions;

    public Member authenticate(String email, String password, String ipAddress) {
        String address = ClientIpResolver.canonicalAddress(ipAddress);
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "로그인 요청의 IP를 확인할 수 없습니다.");
        }

        Verification verification;
        try {
            verification = transactions.write(() -> verify(email, password, address));
        } catch (DataIntegrityViolationException failure) {
            if (blocks.isBlocked(address)) throw blockedIp();
            throw failure;
        }

        // Translate the result after committing the failure count or permanent block.
        return switch (verification.outcome()) {
            case AUTHENTICATED -> verification.member();
            case INVALID_CREDENTIALS -> throw new BadCredentialsException(
                    failureMessage(verification.failures(), "이메일이나 비밀번호를 확인해 주세요."));
            case BLOCKED_ACCOUNT -> throw new DisabledException(
                    failureMessage(verification.failures(), "차단된 계정입니다."));
            case BLOCKED_IP -> throw blockedIp();
        };
    }

    private Verification verify(String email, String password, String address) {
        attempts.ensureCounter(address);
        var counter = attempts.findCounterForUpdate(address).orElseThrow();

        if (blocks.isBlocked(address)) {
            return new Verification(Outcome.BLOCKED_IP, null, 0);
        }
        if (counter.getFailedAttempts() >= ALLOWED_FAILURES) {
            blocks.block(address, null);
            return new Verification(Outcome.BLOCKED_IP, null, 0);
        }

        Member member = members.findMemberOrNull(email);
        if (member == null || !encoder.matches(password, member.getPassword())) {
            return new Verification(Outcome.INVALID_CREDENTIALS, null, counter.recordFailure());
        }
        if (member.isAccessBlocked()) {
            return new Verification(Outcome.BLOCKED_ACCOUNT, null, counter.recordFailure());
        }

        counter.resetFailures();
        return new Verification(Outcome.AUTHENTICATED, member, 0);
    }

    private String failureMessage(int failures, String message) {
        if (failures >= ALLOWED_FAILURES) {
            return message + " 다음 로그인 시도 시 이 IP가 영구 차단됩니다.";
        }
        return message;
    }

    private ResponseStatusException blockedIp() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "차단된 IP입니다.");
    }

    private enum Outcome { AUTHENTICATED, INVALID_CREDENTIALS, BLOCKED_ACCOUNT, BLOCKED_IP }

    private record Verification(Outcome outcome, Member member, int failures) {}
}
