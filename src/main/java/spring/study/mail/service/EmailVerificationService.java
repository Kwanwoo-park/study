package spring.study.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.entity.EmailVerificationPurpose;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration PASSWORD_VERIFIED_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final String EMAIL_CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender emailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${naver.id}")
    private String senderAddress;

    public void send(EmailVerificationPurpose purpose, String subject, String email) {
        String recipient = normalizeEmail(email);
        String cooldownKey = key(purpose, subject, "cooldown");

        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청해 주세요");
        }

        try {
            redisTemplate.delete(key(purpose, subject, "code"));
            redisTemplate.delete(key(purpose, subject, "attempt"));

            if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
                redisTemplate.delete(key(purpose, subject, "verified"));
            }

            String code = createCode(purpose);

            redisTemplate.opsForValue().set(key(purpose, subject, "code"), digest(code), CODE_TTL);

            sendMessage(purpose, recipient, code);
        } catch (RuntimeException exception) {
            redisTemplate.delete(key(purpose, subject, "code"));
            redisTemplate.delete(key(purpose, subject, "attempt"));
            redisTemplate.delete(cooldownKey);

            throw exception;
        }
    }

    public String verify(EmailVerificationPurpose purpose, String subject, String code) {
        verifyCode(purpose, subject, code);

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(tokenKey(purpose, token), subject.trim(), CODE_TTL);

        return token;
    }

    public boolean consume(EmailVerificationPurpose purpose, String subject, String token) {
        if (subject == null || subject.isBlank() || token == null || token.isBlank() || token.length() > 100) {
            return false;
        }

        return subject.trim().equals(redisTemplate.opsForValue().getAndDelete(tokenKey(purpose, token)));
    }

    public void verifyPasswordChange(String memberId, String code) {
        verifyCode(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, code);

        redisTemplate.opsForValue().set(key(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, "verified"), "1", PASSWORD_VERIFIED_TTL);
    }

    public boolean consumePasswordChange(String memberId) {
        return redisTemplate.delete(key(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, "verified"));
    }

    private void verifyCode(EmailVerificationPurpose purpose, String subject, String code) {
        String codeKey = key(purpose, subject, "code");
        String savedCode = redisTemplate.opsForValue().get(codeKey);

        if (savedCode == null || code == null || code.isBlank()) {
            throw invalidCode();
        }

        String attemptKey = key(purpose, subject, "attempt");
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        if (Long.valueOf(1).equals(attempts)) {
            redisTemplate.expire(attemptKey, CODE_TTL);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptKey);

            if (purpose == EmailVerificationPurpose.EMAIL) {
                throw invalidCode();
            }

            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 발급해주세요");
        }

        String suppliedCode = purpose == EmailVerificationPurpose.EMAIL ? code.trim() : code;

        if (!MessageDigest.isEqual(savedCode.getBytes(StandardCharsets.UTF_8), digest(suppliedCode).getBytes(StandardCharsets.UTF_8))) {
            throw invalidCode();
        }

        // Only one concurrent request may exchange the code for verification.
        if (!savedCode.equals(redisTemplate.opsForValue().getAndDelete(codeKey))) {
            throw invalidCode();
        }

        redisTemplate.delete(attemptKey);
    }

    private String createCode(EmailVerificationPurpose purpose) {
        if (purpose != EmailVerificationPurpose.EMAIL) {
            return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        }

        StringBuilder code = new StringBuilder(8);

        for (int index = 0; index < 8; index++) {
            code.append(EMAIL_CODE_CHARACTERS.charAt(random.nextInt(EMAIL_CODE_CHARACTERS.length())));
        }

        return code.toString();
    }

    private void sendMessage(EmailVerificationPurpose purpose, String email, String code) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            String fromAddress = senderAddress.contains("@") ? senderAddress : senderAddress + "@naver.com";

            helper.setTo(email);
            helper.setFrom(fromAddress, "Kwanwoo.site");
            helper.setSubject("Kwanwoo.site " + purpose.getTitle());
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;line-height:1.6">
                      <h2>%s</h2>
                      <p>아래 인증번호를 인증을 요청한 화면에 입력해주세요.</p>
                      <p class="verification-code" style="font-size:24px;font-weight:700;letter-spacing:4px">%s</p>
                      <p>인증번호는 5분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
                    </div>
                    """.formatted(purpose.getTitle(), code), true);
            emailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            throw new IllegalStateException("인증 메일을 발송하지 못했습니다", exception);
        }
    }

    private String key(EmailVerificationPurpose purpose, String subject, String part) {
        if (purpose == EmailVerificationPurpose.EMAIL) {
            String suffix = part.equals("attempt") ? "attempts" : part;
            return purpose.getKeyPrefix() + digest(normalizeEmail(subject)) + ":" + suffix;
        }

        return purpose.getKeyPrefix() + part + ":" + subject;
    }

    private String tokenKey(EmailVerificationPurpose purpose, String token) {
        String part = purpose == EmailVerificationPurpose.EMAIL ? "token:" : "verified:";
        return purpose.getKeyPrefix() + part + digest(token);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 254 || !email.contains("@")) {
            throw new IllegalArgumentException("이메일을 확인해 주세요");
        }
        return email.trim();
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", exception);
        }
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 올바르지 않습니다");
    }
}
