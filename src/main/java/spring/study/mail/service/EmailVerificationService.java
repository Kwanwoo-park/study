package spring.study.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.component.EmailVerificationGuard;
import spring.study.mail.component.Pop3BounceMailbox;
import spring.study.mail.entity.EmailVerificationPurpose;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    public static final Duration CODE_TTL = Duration.ofMinutes(5);
    public static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration PASSWORD_VERIFIED_TTL = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;
    private static final String EMAIL_CODE_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DefaultRedisScript<Long> DELETE_MATCHING_VALUE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> COMPLETE_REGISTRATION_VERIFICATION = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
                return 1
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> RECORD_REGISTRATION_FAILURE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end
            local remaining = redis.call('PTTL', KEYS[1])
            if remaining <= 0 then
                return 0
            end
            local failures = redis.call('INCR', KEYS[2])
            if failures >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 1
            end
            redis.call('PEXPIRE', KEYS[2], remaining)
            return 0
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender emailSender;
    private final Pop3BounceMailbox bounceMailbox;
    private final EmailVerificationGuard verificationGuard;
    private final SecureRandom random = new SecureRandom();

    @Value("${naver.id}")
    private String senderAddress;

    public void send(EmailVerificationPurpose purpose, String subject, String email) {
        InternetAddress recipient = recipientAddress(email);
        verificationGuard.recordRequest(emailDigest(recipient.getAddress()));
        String cooldownKey = key(purpose, subject, "cooldown");

        reserveCooldown(cooldownKey);

        try {
            redisTemplate.delete(key(purpose, subject, "code"));
            redisTemplate.delete(key(purpose, subject, "attempt"));

            if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
                redisTemplate.delete(key(purpose, subject, "verified"));
            }

            String code = createCode(purpose);

            redisTemplate.opsForValue().set(key(purpose, subject, "code"), digest(code), CODE_TTL);

            sendMessage(purpose, recipient, code, null);
        } catch (RuntimeException exception) {
            redisTemplate.delete(key(purpose, subject, "code"));
            redisTemplate.delete(key(purpose, subject, "attempt"));
            redisTemplate.delete(cooldownKey);

            throw exception;
        }
    }

    public String sendRegistrationCode(String email) {
        InternetAddress recipient = recipientAddress(email);
        String address = recipient.getAddress();
        verificationGuard.recordRequest(emailDigest(address));
        String cooldownKey = key(EmailVerificationPurpose.EMAIL, address, "cooldown");
        reserveCooldown(cooldownKey);

        String attemptId = UUID.randomUUID().toString();
        String code = createCode(EmailVerificationPurpose.REGISTRATION);
        try {
            sendMessage(EmailVerificationPurpose.REGISTRATION, recipient, code, registrationMessageId(attemptId));
            redisTemplate.opsForValue().set(
                    key(EmailVerificationPurpose.REGISTRATION, attemptId, "code"),
                    new RegistrationEmail(address, digest(code)).encode(), CODE_TTL);
            return attemptId;
        } catch (RuntimeException exception) {
            redisTemplate.delete(cooldownKey);
            throw exception;
        }
    }

    public void verifyRegistrationCode(String attemptId, String code) {
        if (attemptId == null) throw invalidCode();

        EmailVerificationPurpose purpose = EmailVerificationPurpose.REGISTRATION;
        String codeKey = key(purpose, attemptId, "code");
        String attemptKey = key(purpose, attemptId, "attempt");
        String saved = redisTemplate.opsForValue().get(codeKey);
        RegistrationEmail registration = RegistrationEmail.decode(saved);
        if (registration == null || registration.verified() || code == null || code.isBlank()) throw invalidCode();
        verificationGuard.requireAllowed(emailDigest(registration.address()));

        if (!matchesCode(registration.codeDigest(), code.trim())) {
            Long invalidated = redisTemplate.execute(RECORD_REGISTRATION_FAILURE, List.of(codeKey, attemptKey),
                    saved, Integer.toString(MAX_ATTEMPTS));
            if (Long.valueOf(1).equals(invalidated)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "인증번호를 " + MAX_ATTEMPTS + "회 잘못 입력했습니다. 새 인증번호를 받아 주세요.");
            }
            throw invalidCode();
        }

        rejectBouncedRegistration(attemptId, registration.address(), codeKey, saved);

        String verified = new RegistrationEmail(registration.address(), "").encode();
        Long completed = redisTemplate.execute(COMPLETE_REGISTRATION_VERIFICATION, List.of(codeKey),
                saved, verified, Long.toString(CODE_TTL.toMillis()));
        if (!Long.valueOf(1).equals(completed)) throw invalidCode();
        redisTemplate.delete(attemptKey);
    }

    public String verifiedRegistrationEmail(String attemptId) {
        RegistrationEmail registration = findRegistration(attemptId);
        return registration != null && registration.verified()
                && !verificationGuard.isBlocked(emailDigest(registration.address())) ? registration.address() : null;
    }

    public String pendingRegistrationEmail(String attemptId) {
        RegistrationEmail registration = findRegistration(attemptId);
        return registration != null && !registration.verified()
                && !verificationGuard.isBlocked(emailDigest(registration.address())) ? registration.address() : null;
    }

    private RegistrationEmail findRegistration(String attemptId) {
        if (attemptId == null) return null;
        return RegistrationEmail.decode(redisTemplate.opsForValue()
                .get(key(EmailVerificationPurpose.REGISTRATION, attemptId, "code")));
    }

    public boolean consumeRegistration(String attemptId, String email) {
        if (attemptId == null || email == null) return false;

        String codeKey = key(EmailVerificationPurpose.REGISTRATION, attemptId, "code");
        String saved = redisTemplate.opsForValue().get(codeKey);
        RegistrationEmail registration = RegistrationEmail.decode(saved);
        if (registration == null || !registration.verified() || !registration.address().equals(email)) return false;
        verificationGuard.requireAllowed(emailDigest(registration.address()));

        rejectBouncedRegistration(attemptId, registration.address(), codeKey, saved);
        return deleteMatchingValue(codeKey, saved);
    }

    public void clearRegistration(String attemptId) {
        if (attemptId != null) {
            redisTemplate.delete(key(EmailVerificationPurpose.REGISTRATION, attemptId, "code"));
        }
    }

    private void rejectBouncedRegistration(String attemptId, String address, String codeKey, String saved) {
        if (bounceMailbox.hasBounced(address, registrationMessageId(attemptId))) {
            deleteMatchingValue(codeKey, saved);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "인증 메일이 반송되었습니다. 메일을 받을 수 있는 주소로 다시 인증해 주세요.");
        }
    }

    private String registrationMessageId(String attemptId) {
        return "<" + attemptId + "@kwanwoo.site>";
    }

    private void reserveCooldown(String cooldownKey) {
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 후 다시 요청해 주세요");
        }
    }

    public String verify(EmailVerificationPurpose purpose, String subject, String email, String code) {
        verifyCode(purpose, subject, email, code);

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        if (purpose == EmailVerificationPurpose.APPEAL) {
            // Keep one outstanding, single-use proof per member without a writing deadline.
            redisTemplate.opsForValue().set(key(purpose, subject.trim(), "verified"), digest(token));
        } else {
            redisTemplate.opsForValue().set(tokenKey(purpose, token), subject.trim(), CODE_TTL);
        }

        return token;
    }

    public boolean consume(EmailVerificationPurpose purpose, String subject, String email, String token) {
        if (subject == null || subject.isBlank() || token == null || token.isBlank() || token.length() > 100) {
            return false;
        }
        requireAllowedAddress(email);

        if (purpose == EmailVerificationPurpose.APPEAL) {
            return deleteMatchingValue(key(purpose, subject.trim(), "verified"), digest(token));
        }
        return subject.trim().equals(redisTemplate.opsForValue().getAndDelete(tokenKey(purpose, token)));
    }

    public void verifyPasswordChange(String memberId, String email, String code) {
        verifyCode(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, email, code);

        redisTemplate.opsForValue().set(key(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, "verified"), "1", PASSWORD_VERIFIED_TTL);
    }

    public boolean consumePasswordChange(String memberId, String email) {
        requireAllowedAddress(email);
        return redisTemplate.delete(key(EmailVerificationPurpose.PASSWORD_CHANGE, memberId, "verified"));
    }

    private void verifyCode(EmailVerificationPurpose purpose, String subject, String email, String code) {
        requireAllowedAddress(email);
        String codeKey = key(purpose, subject, "code");
        String savedCode = redisTemplate.opsForValue().get(codeKey);
        requireMatchingCode(purpose, subject, code, savedCode);

        // Only one concurrent request may exchange the code for verification.
        if (!deleteMatchingValue(codeKey, savedCode)) throw invalidCode();
        redisTemplate.delete(key(purpose, subject, "attempt"));
    }

    private void requireMatchingCode(EmailVerificationPurpose purpose, String subject, String code, String savedDigest) {
        if (savedDigest == null || code == null || code.isBlank()) {
            throw invalidCode();
        }

        String attemptKey = key(purpose, subject, "attempt");
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);

        if (Long.valueOf(1).equals(attempts)) {
            redisTemplate.expire(attemptKey, CODE_TTL);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            deleteMatchingValue(key(purpose, subject, "code"), savedDigest);
            redisTemplate.delete(attemptKey);

            if (purpose == EmailVerificationPurpose.EMAIL) {
                throw invalidCode();
            }

            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 발급해주세요");
        }

        String suppliedCode = purpose == EmailVerificationPurpose.EMAIL ? code.trim() : code;

        if (!matchesCode(savedDigest, suppliedCode)) {
            throw invalidCode();
        }
    }

    private boolean matchesCode(String savedDigest, String code) {
        return MessageDigest.isEqual(savedDigest.getBytes(StandardCharsets.UTF_8), digest(code).getBytes(StandardCharsets.UTF_8));
    }

    private boolean deleteMatchingValue(String key, String expectedValue) {
        Long deleted = redisTemplate.execute(DELETE_MATCHING_VALUE, List.of(key), expectedValue);
        return Long.valueOf(1).equals(deleted);
    }

    private String createCode(EmailVerificationPurpose purpose) {
        if (purpose == EmailVerificationPurpose.APPEAL || purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
            return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        }

        StringBuilder code = new StringBuilder(8);

        for (int index = 0; index < 8; index++) {
            code.append(EMAIL_CODE_CHARACTERS.charAt(random.nextInt(EMAIL_CODE_CHARACTERS.length())));
        }

        return code.toString();
    }

    private void sendMessage(EmailVerificationPurpose purpose, InternetAddress recipient, String code, String messageId) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            String fromAddress = senderAddress.contains("@") ? senderAddress : senderAddress + "@naver.com";

            helper.setTo(recipient);
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
            if (messageId != null) message.setHeader("Message-ID", messageId);
            emailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            throw new IllegalStateException("인증 메일을 발송하지 못했습니다", exception);
        }
    }

    private String key(EmailVerificationPurpose purpose, String subject, String part) {
        if (purpose == EmailVerificationPurpose.EMAIL) {
            String suffix = part.equals("attempt") ? "attempts" : part;
            return purpose.getKeyPrefix() + digest(recipientAddress(subject).getAddress()) + ":" + suffix;
        }

        return purpose.getKeyPrefix() + part + ":" + subject;
    }

    private String tokenKey(EmailVerificationPurpose purpose, String token) {
        String part = purpose == EmailVerificationPurpose.EMAIL ? "token:" : "verified:";
        return purpose.getKeyPrefix() + part + digest(token);
    }

    private InternetAddress recipientAddress(String email) {
        if (email == null || email.isBlank() || email.length() > 254 || !email.contains("@")
                || email.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("이메일을 확인해 주세요");
        }
        String address = email.trim();
        try {
            InternetAddress recipient = new InternetAddress(address, true);
            recipient.validate();
            if (!address.equals(recipient.getAddress()) || recipient.getPersonal() != null || recipient.isGroup()) {
                throw new AddressException();
            }
            return recipient;
        } catch (AddressException invalidAddress) {
            throw new IllegalArgumentException("이메일을 확인해 주세요");
        }
    }

    private void requireAllowedAddress(String email) {
        verificationGuard.requireAllowed(emailDigest(recipientAddress(email).getAddress()));
    }

    private String emailDigest(String address) {
        return digest(address.toLowerCase(Locale.ROOT));
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

    private record RegistrationEmail(String address, String codeDigest) {
        static RegistrationEmail decode(String value) {
            if (value == null) return null;
            String[] fields = value.split("\n", 2);
            return fields.length == 2 ? new RegistrationEmail(fields[0], fields[1]) : null;
        }

        boolean verified() {
            return codeDigest.isEmpty();
        }

        String encode() {
            return address + "\n" + codeDigest;
        }
    }
}
