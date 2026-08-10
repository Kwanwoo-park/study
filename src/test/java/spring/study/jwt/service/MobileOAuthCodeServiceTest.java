package spring.study.jwt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import spring.study.member.entity.Member;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileOAuthCodeServiceTest {
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private MobileOAuthCodeService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new MobileOAuthCodeService(redisTemplate);
    }

    @Test
    void createsShortLivedCodeAndConsumesItAtomically() {
        Member member = Member.builder().id(7L).build();

        String code = service.create(member);

        verify(valueOperations).set("auth:mobile-oauth:" + code, "7", Duration.ofMinutes(2));
        when(valueOperations.getAndDelete("auth:mobile-oauth:" + code)).thenReturn("7");
        assertThat(service.consume(code)).contains(7L);
        verify(valueOperations).getAndDelete("auth:mobile-oauth:" + code);
        assertThat(code).matches("[0-9a-f]{32}");
    }

    @Test
    void rejectsMissingOrExpiredCode() {
        assertThat(service.consume(null)).isEmpty();
        assertThat(service.consume(" ")).isEmpty();
        when(valueOperations.getAndDelete("auth:mobile-oauth:expired")).thenReturn(null);
        assertThat(service.consume("expired")).isEmpty();
    }
}
