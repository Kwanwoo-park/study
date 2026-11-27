package spring.study.mail.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.component.EmailVerificationGuard;
import spring.study.mail.component.Pop3BounceMailbox;
import spring.study.mail.entity.EmailVerificationPurpose;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
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
    private final Pop3BounceMailbox bounce = mock(Pop3BounceMailbox.class);
    private final EmailVerificationGuard guard = mock(EmailVerificationGuard.class);
    private final EmailVerificationService service = new EmailVerificationService(redis, sender, bounce, guard);
    private MimeMessage message;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "senderAddress", "sender@example.test");
    }

    @ParameterizedTest
    @EnumSource(value = EmailVerificationPurpose.class, names = "REGISTRATION", mode = EnumSource.Mode.EXCLUDE)
    void codesExpireAndSingleUseProofsFollowTheirPurposePolicy(EmailVerificationPurpose purpose) throws Exception {
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
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(codeKey(purpose))), eq(hash)))
                .thenReturn(1L, 0L);
        when(values.increment(attemptKey(purpose))).thenReturn(1L);

        if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
            service.verifyPasswordChange(subject, EMAIL, code);
            verify(values).set("verification:password-change:verified:7", "1", Duration.ofMinutes(10));
            when(redis.delete("verification:password-change:verified:7")).thenReturn(true, false);
            assertThat(service.consumePasswordChange(subject, EMAIL)).isTrue();
            assertThat(service.consumePasswordChange(subject, EMAIL)).isFalse();
            assertThatThrownBy(() -> service.verifyPasswordChange(subject, EMAIL, code)).isInstanceOf(ResponseStatusException.class);
        } else {
            String token = service.verify(purpose, subject, EMAIL, code);
            assertThat(token).hasSize(43);
            if (purpose == EmailVerificationPurpose.APPEAL) {
                String proofKey = "verification:appeal:verified:7";
                verify(values).set(proofKey, digest(token));
                when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(proofKey)), eq(digest(token))))
                        .thenReturn(1L, 0L);
                assertThat(service.consume(purpose, "8", "other@example.test", token)).isFalse();
                assertThat(service.consume(purpose, subject, EMAIL, "wrong-token")).isFalse();
            } else {
                String tokenKey = purpose.getKeyPrefix() + "token:" + digest(token);
                verify(values).set(tokenKey, subject, Duration.ofMinutes(5));
                when(values.getAndDelete(tokenKey)).thenReturn(subject, (String) null);
            }
            assertThat(service.consume(purpose, subject, EMAIL, token)).isTrue();
            assertThat(service.consume(purpose, subject, EMAIL, token)).isFalse();
            assertThatThrownBy(() -> service.verify(purpose, subject, EMAIL, code)).isInstanceOf(ResponseStatusException.class);
        }
        verify(redis, atLeastOnce()).expire(attemptKey(purpose), Duration.ofMinutes(5));
    }

    @ParameterizedTest
    @EnumSource(value = EmailVerificationPurpose.class, names = "REGISTRATION", mode = EnumSource.Mode.EXCLUDE)
    void missingWrongAndOverLimitCodesCannotCreateAProof(EmailVerificationPurpose purpose) throws Exception {
        assertThatThrownBy(() -> verifyCode(purpose, "123456")).isInstanceOf(ResponseStatusException.class);
        when(values.get(codeKey(purpose))).thenReturn(digest("123456"));
        when(values.increment(attemptKey(purpose))).thenReturn(1L, 6L);

        assertThatThrownBy(() -> verifyCode(purpose, "wrong")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> verifyCode(purpose, "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(
                                purpose == EmailVerificationPurpose.EMAIL ? HttpStatus.BAD_REQUEST : HttpStatus.TOO_MANY_REQUESTS));
        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(codeKey(purpose))), eq(digest("123456")));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
        verify(values, never()).set(anyString(), anyString());
    }

    @Test
    void registrationActivatesCodeAfterSendingToOneAddress() throws Exception {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(1)))).thenReturn(true);
        String attempt = service.sendRegistrationCode(EMAIL);
        String id = "<" + attempt + "@kwanwoo.site>";
        assertThat(message.getMessageID()).isEqualTo(id);
        assertThat(message.getAllRecipients()).singleElement().hasToString(EMAIL);
        Matcher matcher = Pattern.compile("<p class=\"verification-code\"[^>]*>([^<]+)</p>")
                .matcher((String) message.getContent());
        assertThat(matcher.find()).isTrue();

        var order = inOrder(sender, values);
        order.verify(sender).send(message);
        order.verify(values).set(registrationKey(attempt), EMAIL + "\n" + digest(matcher.group(1)), Duration.ofMinutes(5));
    }

    @Test
    void failedSmtpSendLeavesNoActiveRegistrationCode() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofMinutes(1)))).thenReturn(true);
        doThrow(new MailSendException("SMTP failure")).when(sender).send(message);

        assertThatThrownBy(() -> service.sendRegistrationCode(EMAIL)).isInstanceOf(IllegalStateException.class);
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void registrationAndPasswordRecoveryCountRequestsForTheSameAddressBeforeCooldown() {
        String emailDigest = digest(EMAIL);
        doNothing().doNothing().doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(guard).recordRequest(eq(emailDigest), anyString());
        when(values.setIfAbsent(anyString(), eq("1"), eq(EmailVerificationService.RESEND_COOLDOWN)))
                .thenReturn(true, false);

        service.sendRegistrationCode(" Member@Example.Test ");
        assertThatThrownBy(() -> service.send(EmailVerificationPurpose.EMAIL, EMAIL, EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> service.sendRegistrationCode(EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(guard).recordRequest(emailDigest, "Member@Example.Test");
        verify(guard, times(2)).recordRequest(emailDigest, EMAIL);
        verify(sender, times(1)).send(message);
    }

    @Test
    void manualBlockUsesTheSameAddressValidationAndDigestAsVerification() {
        service.blockEmail(" Member@Example.Test ");
        assertThatThrownBy(() -> service.blockEmail("Member <" + EMAIL + ">"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(guard).block(digest(EMAIL), "Member@Example.Test");
        verifyNoMoreInteractions(guard);
        verifyNoInteractions(sender);
    }

    @Test
    void blockedAddressCannotVerifyOrUseAnEarlierRegistrationProof() {
        String code = "a123A567";
        when(values.get(registrationKey("pending"))).thenReturn(EMAIL + "\n" + digest(code));
        when(values.get(registrationKey("verified"))).thenReturn(EMAIL + "\n");
        when(guard.isBlocked(digest(EMAIL))).thenReturn(true);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(guard).requireAllowed(digest(EMAIL));

        assertThat(service.pendingRegistrationEmail("pending")).isNull();
        assertThat(service.verifiedRegistrationEmail("verified")).isNull();
        assertThatThrownBy(() -> service.verifyRegistrationCode("pending", code))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.consumeRegistration("verified", EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(bounce);
    }

    @Test
    void correctCodeAfterFourWrongCodesBecomesAnEmailBoundSingleUseRegistration() {
        String attempt = "attempt";
        String key = registrationKey(attempt);
        String code = "a123A567";
        String pending = EMAIL + "\n" + digest(code);
        String verified = EMAIL + "\n";
        when(values.get(key)).thenReturn(pending, pending, pending, pending, pending, verified, verified, verified, null);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)), eq(pending),
                eq(verified), eq("300000"))).thenReturn(1L);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)), eq(verified))).thenReturn(1L);

        for (int failure = 0; failure < 4; failure++) {
            assertThatThrownBy(() -> service.verifyRegistrationCode(attempt, "b123B567"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        service.verifyRegistrationCode(attempt, code);
        assertThatThrownBy(() -> service.verifyRegistrationCode(attempt, code))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.consumeRegistration(attempt, "another@example.test")).isFalse();
        assertThat(service.consumeRegistration(attempt, EMAIL)).isTrue();
        assertThat(service.consumeRegistration(attempt, EMAIL)).isFalse();
        verify(bounce, times(2)).hasBounced(EMAIL, "<attempt@kwanwoo.site>");
    }

    @Test
    void fifthWrongCodeInvalidatesRegistrationBeforeAnyFurtherVerification() {
        String attempt = "attempt";
        String key = registrationKey(attempt);
        String code = "a123A567";
        String pending = EMAIL + "\n" + digest(code);
        when(values.get(key)).thenReturn(pending, pending, pending, pending, pending, null);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(key, "verification:registration:attempt:" + attempt)), eq(pending), eq("5")))
                .thenReturn(0L, 0L, 0L, 0L, 1L);

        for (int failure = 0; failure < 4; failure++) {
            assertThatThrownBy(() -> service.verifyRegistrationCode(attempt, "b123B567"))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        assertThatThrownBy(() -> service.verifyRegistrationCode(attempt, "b123B567"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> service.verifyRegistrationCode(attempt, code))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.consumeRegistration(attempt, EMAIL)).isFalse();
        verifyNoInteractions(bounce);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void matchingBounceInvalidatesRegistrationBeforeVerificationOrCreation(boolean verified) {
        String key = registrationKey("attempt");
        String code = "a123A567";
        String value = EMAIL + "\n" + (verified ? "" : digest(code));
        when(values.get(key)).thenReturn(value);
        when(bounce.hasBounced(EMAIL, "<attempt@kwanwoo.site>")).thenReturn(true);

        assertThatThrownBy(() -> {
            if (verified) service.consumeRegistration("attempt", EMAIL);
            else service.verifyRegistrationCode("attempt", code);
        }).isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(redis).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)), eq(value));
    }

    @Test
    void pop3ErrorPreservesVerifiedRegistrationForRetry() {
        String key = registrationKey("attempt");
        String value = EMAIL + "\n";
        when(values.get(key)).thenReturn(value);
        when(bounce.hasBounced(EMAIL, "<attempt@kwanwoo.site>"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .thenReturn(false);
        when(redis.execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)), eq(value))).thenReturn(1L);

        assertThatThrownBy(() -> service.consumeRegistration("attempt", EMAIL))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(redis, never()).execute(ArgumentMatchers.<RedisScript<Long>>any(), eq(List.of(key)), eq(value));
        assertThat(service.consumeRegistration("attempt", EMAIL)).isTrue();
    }

    private String registrationKey(String attempt) {
        return "verification:registration:code:" + attempt;
    }

    private void verifyCode(EmailVerificationPurpose purpose, String code) {
        if (purpose == EmailVerificationPurpose.PASSWORD_CHANGE) {
            service.verifyPasswordChange(subject(purpose), EMAIL, code);
        } else {
            service.verify(purpose, subject(purpose), EMAIL, code);
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
