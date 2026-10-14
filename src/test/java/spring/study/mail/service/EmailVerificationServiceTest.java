package spring.study.mail.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    private final RegisterMail mail = mock(RegisterMail.class);
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final EmailVerificationService service = new EmailVerificationService(mail, redis);

    @Test
    void codeAndSingleUseProofExpireAfterFiveMinutes() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(mail.sendSimpleMessage("member@example.test")).thenReturn("mail-code");
        service.send("member@example.test");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("mail-code".getBytes(StandardCharsets.UTF_8)));
        verify(values).set(endsWith(":code"), eq(digest), eq(Duration.ofMinutes(5)));
        when(values.get(endsWith(":code"))).thenReturn(digest);
        when(values.getAndDelete(endsWith(":code"))).thenReturn(digest);
        when(values.increment(anyString())).thenReturn(1L);

        String token = service.verify("member@example.test", "mail-code");
        verify(values).set(contains("token:"), eq("member@example.test"), eq(Duration.ofMinutes(5)));
        when(values.getAndDelete(contains("token:"))).thenReturn("member@example.test", (String) null);
        assertThat(service.consume("member@example.test", token)).isTrue();
        assertThat(service.consume("member@example.test", token)).isFalse();
    }

    @Test
    void missingOrExpiredCodeAndProofAreRejected() {
        when(redis.opsForValue()).thenReturn(values);
        assertThatThrownBy(() -> service.verify("member@example.test", "wrong-code"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.consume("member@example.test", "expired-token")).isFalse();
    }
}
