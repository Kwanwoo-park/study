package spring.study.appeal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.service.AppealVerificationMailService;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppealVerificationServiceTest {
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AppealVerificationMailService mailService = mock(AppealVerificationMailService.class);
    private AppealVerificationService service;
    private Member member;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AppealVerificationService(redisTemplate, memberRepository, mailService);
        member = Member.builder().id(7L).email("member@example.com").build();
    }

    @Test
    void verificationCodeAndOneTimeGrantShouldBothExpireAfterFiveMinutes() {
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(valueOperations.setIfAbsent(
                "verification:appeal:cooldown:7", "1", Duration.ofMinutes(1)))
                .thenReturn(true);

        service.sendCode(" member@example.com ");

        ArgumentCaptor<String> mailedCode = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> savedHash = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("member@example.com"), mailedCode.capture());
        verify(valueOperations).set(
                eq("verification:appeal:code:7"),
                savedHash.capture(),
                eq(Duration.ofMinutes(5)));
        assertThat(mailedCode.getValue()).matches("\\d{6}");
        assertThat(savedHash.getValue()).matches("[0-9a-f]{64}").isNotEqualTo(mailedCode.getValue());

        when(valueOperations.get("verification:appeal:code:7")).thenReturn(savedHash.getValue());
        when(valueOperations.increment("verification:appeal:attempt:7")).thenReturn(1L);

        String token = service.verifyCode("member@example.com", mailedCode.getValue());

        verify(redisTemplate).expire("verification:appeal:attempt:7", Duration.ofMinutes(5));
        ArgumentCaptor<String> verifiedKey = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(verifiedKey.capture(), eq("7"), eq(Duration.ofMinutes(5)));
        assertThat(token).hasSize(43);
        assertThat(verifiedKey.getValue()).matches("verification:appeal:verified:[0-9a-f]{64}");
        assertThat(verifiedKey.getValue()).doesNotContain(token);

        when(valueOperations.getAndDelete(verifiedKey.getValue())).thenReturn("7");

        assertThat(service.consumeVerification("member@example.com", token)).isSameAs(member);
        verify(valueOperations).getAndDelete(verifiedKey.getValue());
    }

    @Test
    void wrongCodeShouldNotIssueVerificationGrant() {
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(valueOperations.get("verification:appeal:code:7"))
                .thenReturn("6bb61e3b7bce0931da574d19d1d82c88c84b7a86d8fc3fdb2e5f4c08a6d3d5f8");
        when(valueOperations.increment("verification:appeal:attempt:7")).thenReturn(2L);

        assertThatThrownBy(() -> service.verifyCode("member@example.com", "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(valueOperations, never()).set(
                org.mockito.ArgumentMatchers.startsWith("verification:appeal:verified:"),
                eq("7"),
                eq(Duration.ofMinutes(5)));
    }

    @Test
    void repeatedMailRequestWithinOneMinuteShouldBeRejected() {
        when(memberRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(valueOperations.setIfAbsent(
                "verification:appeal:cooldown:7", "1", Duration.ofMinutes(1)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.sendCode("member@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verifyNoInteractions(mailService);
    }

    @Test
    void unknownEmailShouldNotRevealWhetherAnAccountExists() {
        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.sendCode("unknown@example.com");

        verifyNoInteractions(mailService);
        verify(valueOperations, never()).setIfAbsent(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
