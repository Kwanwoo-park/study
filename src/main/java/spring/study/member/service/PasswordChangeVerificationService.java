package spring.study.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.service.PasswordChangeMailService;
import spring.study.member.entity.Member;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordChangeVerificationService {
    private static final String KEY_PREFIX = "verification:password-change:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordChangeMailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode(Member member) {
        String cooldownKey = cooldownKey(member.getId());
        Boolean allowed = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(allowed)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청할 수 있습니다");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        try {
            redisTemplate.opsForValue().set(codeKey(member.getId()), digest(code), CODE_TTL);
            redisTemplate.delete(verifiedKey(member.getId()));
            redisTemplate.delete(attemptKey(member.getId()));
            mailService.send(member.getEmail(), code);
        } catch (RuntimeException exception) {
            redisTemplate.delete(codeKey(member.getId()));
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
    }

    public void verifyCode(Member member, String code) {
        Long memberId = member.getId();
        String savedCode = redisTemplate.opsForValue().get(codeKey(memberId));
        if (savedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 발급되지 않았습니다");
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptKey(memberId));
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptKey(memberId), CODE_TTL);
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey(memberId));
            redisTemplate.delete(attemptKey(memberId));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 인증번호를 다시 발급해주세요");
        }

        if (!MessageDigest.isEqual(
                savedCode.getBytes(StandardCharsets.UTF_8),
                digest(code).getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다");
        }

        redisTemplate.delete(codeKey(memberId));
        redisTemplate.delete(attemptKey(memberId));
        redisTemplate.opsForValue().set(verifiedKey(memberId), "1", VERIFIED_TTL);
    }

    public boolean consumeVerification(Member member) {
        return Boolean.TRUE.equals(redisTemplate.delete(verifiedKey(member.getId())));
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

    private String codeKey(Long memberId) {
        return KEY_PREFIX + "code:" + memberId;
    }

    private String verifiedKey(Long memberId) {
        return KEY_PREFIX + "verified:" + memberId;
    }

    private String attemptKey(Long memberId) {
        return KEY_PREFIX + "attempt:" + memberId;
    }

    private String cooldownKey(Long memberId) {
        return KEY_PREFIX + "cooldown:" + memberId;
    }
}
