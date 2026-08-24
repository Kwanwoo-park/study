package spring.study.appeal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.service.AppealVerificationMailService;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AppealVerificationService {
    private static final String KEY_PREFIX = "verification:appeal:";
    private static final Duration VERIFICATION_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;
    private final AppealVerificationMailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode(String email) {
        Member member = findMemberOrNull(email);
        if (member == null) return;

        String cooldownKey = cooldownKey(member.getId());
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "인증번호는 1분 후 다시 요청할 수 있습니다"
            );
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        try {
            redisTemplate.opsForValue().set(codeKey(member.getId()), digest(code), VERIFICATION_TTL);
            redisTemplate.delete(attemptKey(member.getId()));
            mailService.send(member.getEmail(), code);
        } catch (RuntimeException exception) {
            redisTemplate.delete(codeKey(member.getId()));
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
    }

    public String verifyCode(String email, String code) {
        Member member = findMember(email);
        String savedCode = redisTemplate.opsForValue().get(codeKey(member.getId()));
        if (savedCode == null) {
            throw invalidCode();
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptKey(member.getId()));
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptKey(member.getId()), VERIFICATION_TTL);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey(member.getId()));
            redisTemplate.delete(attemptKey(member.getId()));
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 발급해주세요"
            );
        }

        if (!MessageDigest.isEqual(
                savedCode.getBytes(StandardCharsets.UTF_8),
                digest(code).getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다");
        }

        redisTemplate.delete(codeKey(member.getId()));
        redisTemplate.delete(attemptKey(member.getId()));

        String verificationToken = newVerificationToken();
        redisTemplate.opsForValue().set(
                verifiedTokenKey(verificationToken),
                String.valueOf(member.getId()),
                VERIFICATION_TTL
        );
        return verificationToken;
    }

    public Member consumeVerification(String email, String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            throw verificationRequired();
        }

        Member member = findMember(email);
        String verifiedMemberId = redisTemplate.opsForValue()
                .getAndDelete(verifiedTokenKey(verificationToken));
        if (verifiedMemberId == null || !verifiedMemberId.equals(String.valueOf(member.getId()))) {
            throw verificationRequired();
        }
        return member;
    }

    private Member findMember(String email) {
        Member member = findMemberOrNull(email);
        if (member == null) throw invalidCode();
        return member;
    }

    private Member findMemberOrNull(String email) {
        if (email == null || email.isBlank()) return null;
        return memberRepository.findByEmail(email.trim()).orElse(null);
    }

    private String newVerificationToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", exception);
        }
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "인증번호가 만료되었거나 올바르지 않습니다"
        );
    }

    private ResponseStatusException verificationRequired() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "5분 이내에 이메일 인증을 완료한 후 상소문을 제출해주세요"
        );
    }

    private String codeKey(Long memberId) {
        return KEY_PREFIX + "code:" + memberId;
    }

    private String attemptKey(Long memberId) {
        return KEY_PREFIX + "attempt:" + memberId;
    }

    private String cooldownKey(Long memberId) {
        return KEY_PREFIX + "cooldown:" + memberId;
    }

    private String verifiedTokenKey(String verificationToken) {
        return KEY_PREFIX + "verified:" + digest(verificationToken);
    }
}
