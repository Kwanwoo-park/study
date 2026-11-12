package spring.study.mail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.entity.EmailVerificationPurpose;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    private static final String EMAIL = "member@example.test";
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailVerificationService service = new EmailVerificationService(redis, sender);
    private MimeMessage message;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "senderAddress", "sender@example.test");
    }

    @ParameterizedTest
    @EnumSource(EmailVerificationPurpose.class)
    void codeAndPurposeSpecificSingleUseProofKeepTheirExpiry(EmailVerificationPurpose purpose) throws Exception {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(1)))).thenReturn(true);
        String subject = subject(purpose);
        service.send(purpose, subject, EMAIL);

        verify(sender).send(message);
        assertThat(message.getSubject()).isEqualTo("Kwanwoo.site " + purpose.getTitle());
        Matcher matcher = Pattern.compile("<p class=\"verification-code\"[^>]*>([^<]+)</p>").matcher((String) message.getContent());
        assertThat(matcher.find()).isTrue();
        String code = matcher.group(1);
        assertThat(code).matches(purpose == EmailVerificationPurpose.EMAIL ? "[A-Za-z0-9]{8}" : "\\d{6}");
        String hash = digest(code);
        verify(values).set(codeKey(purpose), hash, Duration.ofMinutes(5));
        assertThat(hash).isNotEqualTo(code);
        when(values.get(codeKey(purpose))).thenReturn(hash);
        when(values.getAndDelete(codeKey(purpose))).thenReturn(hash, (String) null);
        when(values.increment(attemptKey(purpose))).thenReturn(1L);

        if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
            service.verifyPasswordChange(subject, code);
            verify(values).set("verification:password-change:verified:7", "1", Duration.ofMinutes(10));
            when(redis.delete("verification:password-change:verified:7")).thenReturn(true, false);
            assertThat(service.consumePasswordChange(subject)).isTrue();
            assertThat(service.consumePasswordChange(subject)).isFalse();
            assertThatThrownBy(() -> service.verifyPasswordChange(subject, code)).isInstanceOf(ResponseStatusException.class);
        } else {
            String token = service.verify(purpose, subject, code);
            assertThat(token).hasSize(43);
            String tokenKey = purpose.getKeyPrefix() + (purpose == EmailVerificationPurpose.EMAIL ? "token:" : "verified:") + digest(token);
            verify(values).set(tokenKey, subject, Duration.ofMinutes(5));
            when(values.getAndDelete(tokenKey)).thenReturn(subject, (String) null);
            assertThat(service.consume(purpose, subject, token)).isTrue();
            assertThat(service.consume(purpose, subject, token)).isFalse();
            assertThatThrownBy(() -> service.verify(purpose, subject, code)).isInstanceOf(ResponseStatusException.class);
        }
        verify(redis, atLeastOnce()).expire(attemptKey(purpose), Duration.ofMinutes(5));
    }

    @ParameterizedTest
    @EnumSource(EmailVerificationPurpose.class)
    void missingWrongAndOverLimitCodesCannotCreateAProof(EmailVerificationPurpose purpose) throws Exception {
        assertThatThrownBy(() -> verifyCode(purpose, "123456")).isInstanceOf(ResponseStatusException.class);
        when(values.get(codeKey(purpose))).thenReturn(digest("123456"));
        when(values.increment(attemptKey(purpose))).thenReturn(1L, 6L);

        assertThatThrownBy(() -> verifyCode(purpose, "wrong")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> verifyCode(purpose, "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(
                                purpose == EmailVerificationPurpose.EMAIL ? HttpStatus.BAD_REQUEST : HttpStatus.TOO_MANY_REQUESTS));
        verify(redis).delete(codeKey(purpose));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    private void verifyCode(EmailVerificationPurpose purpose, String code) {
        if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
            service.verifyPasswordChange(subject(purpose), code);
        } else {
            service.verify(purpose, subject(purpose), code);
        }
    }

    private String subject(EmailVerificationPurpose purpose) {
        return purpose == EmailVerificationPurpose.EMAIL ? EMAIL : "7";
    }

    private String codeKey(EmailVerificationPurpose purpose) {
        return key(purpose, "code");
    }

    private String attemptKey(EmailVerificationPurpose purpose) {
        return key(purpose, "attempt");
    }

    private String key(EmailVerificationPurpose purpose, String part) {
        if (purpose == EmailVerificationPurpose.EMAIL) {
            return purpose.getKeyPrefix() + digest(EMAIL) + ":" + (part.equals("attempt") ? "attempts" : part);
        }
        return purpose.getKeyPrefix() + part + ":7";
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
