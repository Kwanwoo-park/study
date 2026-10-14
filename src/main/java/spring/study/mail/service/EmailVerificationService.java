package spring.study.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final String PREFIX = "verification:email:";
    private static final Duration TTL = Duration.ofMinutes(5);
    private final RegisterMail mail;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public void send(String email) throws Exception {
        String key = emailKey(email);
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key + ":cooldown", "1", Duration.ofMinutes(1)))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청해 주세요");
        }
        redisTemplate.delete(key + ":code");
        redisTemplate.delete(key + ":attempts");
        try {
            String code = mail.sendSimpleMessage(email.trim());
            redisTemplate.opsForValue().set(key + ":code", digest(code), TTL);
        } catch (Exception error) {
            redisTemplate.delete(key + ":cooldown");
            throw error;
        }
    }

    public String verify(String email, String code) {
        String key = emailKey(email);
        String saved = redisTemplate.opsForValue().get(key + ":code");
        if (saved == null || code == null || code.isBlank()) throw invalidCode();
        Long attempts = redisTemplate.opsForValue().increment(key + ":attempts");
        if (Long.valueOf(1).equals(attempts)) redisTemplate.expire(key + ":attempts", TTL);
        if (attempts != null && attempts > 5) {
            redisTemplate.delete(key + ":code");
            throw invalidCode();
        }
        if (!MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8), digest(code.trim()).getBytes(StandardCharsets.UTF_8))) {
            throw invalidCode();
        }
        // Only one concurrent verification may consume this code.
        if (!saved.equals(redisTemplate.opsForValue().getAndDelete(key + ":code"))) throw invalidCode();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redisTemplate.opsForValue().set(PREFIX + "token:" + digest(token), email.trim(), TTL);
        return token;
    }

    public boolean consume(String email, String token) {
        if (email == null || token == null || token.isBlank() || token.length() > 100) return false;
        return email.trim().equals(redisTemplate.opsForValue().getAndDelete(PREFIX + "token:" + digest(token)));
    }

    private String emailKey(String email) {
        if (email == null || email.isBlank() || email.length() > 254 || !email.contains("@")) {
            throw new IllegalArgumentException("이메일을 확인해 주세요");
        }
        return PREFIX + digest(email.trim());
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 올바르지 않습니다. 다시 인증해 주세요");
    }
}
