package spring.study.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.service.PasswordChangeMailService;
import spring.study.member.entity.Member;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordChangeVerificationServiceTest {
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final PasswordChangeMailService mailService = mock(PasswordChangeMailService.class);
    private PasswordChangeVerificationService service;
    private Member member;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new PasswordChangeVerificationService(redisTemplate, mailService);
        member = Member.builder().id(7L).email("member@example.com").build();
    }

    @Test
    void sentCodeShouldBeHashedInRedisAndVerifiedAsOneTimeGrant() {
        when(valueOperations.setIfAbsent(
                "verification:password-change:cooldown:7", "1", Duration.ofMinutes(1)))
                .thenReturn(true);

        service.sendCode(member);

        ArgumentCaptor<String> mailedCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("member@example.com"), mailedCode.capture());
        verify(valueOperations).set(
                eq("verification:password-change:code:7"),
                savedHash.capture(),
                eq(Duration.ofMinutes(5)));
        assertThat(mailedCode.getValue()).matches("\\d{6}");
        assertThat(savedHash.getValue()).matches("[0-9a-f]{64}").isNotEqualTo(mailedCode.getValue());

        when(valueOperations.get("verification:password-change:code:7")).thenReturn(savedHash.getValue());
        when(valueOperations.increment("verification:password-change:attempt:7")).thenReturn(1L);

        service.verifyCode(member, mailedCode.getValue());

        verify(redisTemplate).expire("verification:password-change:attempt:7", Duration.ofMinutes(5));
        verify(valueOperations).set(
                "verification:password-change:verified:7", "1", Duration.ofMinutes(10));
    }

    @Test
    void wrongCodeShouldNotCreateVerificationGrant() {
        when(valueOperations.get("verification:password-change:code:7"))
                .thenReturn("6bb61e3b7bce0931da574d19d1d82c88c84b7a86d8fc3fdb2e5f4c08a6d3d5f8");
        when(valueOperations.increment("verification:password-change:attempt:7")).thenReturn(2L);

        assertThatThrownBy(() -> service.verifyCode(member, "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        org.mockito.Mockito.verify(valueOperations, org.mockito.Mockito.never()).set(
                eq("verification:password-change:verified:7"),
                eq("1"),
                eq(Duration.ofMinutes(10)));
    }

    @Test
    void verificationGrantShouldBeConsumedAtomically() {
        when(redisTemplate.delete("verification:password-change:verified:7")).thenReturn(true);

        assertThat(service.consumeVerification(member)).isTrue();

        verify(redisTemplate).delete("verification:password-change:verified:7");
    }

    @Test
    void repeatedMailRequestWithinOneMinuteShouldBeRejected() {
        when(valueOperations.setIfAbsent(
                "verification:password-change:cooldown:7", "1", Duration.ofMinutes(1)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.sendCode(member))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verifyNoInteractions(mailService);
    }
}
