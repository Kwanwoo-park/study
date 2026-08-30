package spring.study.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;
import spring.study.common.dto.IpLocationResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IpLocationServiceTest {
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RestTemplate restTemplate;
    private IpLocationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        restTemplate = mock(RestTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new IpLocationService(
                redisTemplate,
                restTemplate,
                new ObjectMapper(),
                "https://ipinfo.io",
                ""
        );
    }

    @Test
    void publicIpReturnsCountryRegionAndCity() {
        when(valueOperations.get("ip-location:8.8.8.8")).thenReturn(null);
        when(restTemplate.getForObject(any(URI.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn("{\"country\":\"KR\",\"region\":\"Seoul\",\"city\":\"Seoul\"}");

        IpLocationResponse response = service.find("8.8.8.8");

        assertThat(response.available()).isTrue();
        assertThat(response.country()).isEqualTo("대한민국");
        assertThat(response.displayName()).isEqualTo("대한민국 · Seoul");
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("ip-location:8.8.8.8"),
                any(String.class),
                org.mockito.ArgumentMatchers.any(java.time.Duration.class)
        );
    }

    @Test
    void privateIpDoesNotCallExternalApi() {
        IpLocationResponse response = service.find("127.0.0.1");

        assertThat(response.displayName()).isEqualTo("내부 네트워크");
        verify(restTemplate, never()).getForObject(any(URI.class), org.mockito.ArgumentMatchers.eq(String.class));
    }

    @Test
    void invalidIpDoesNotCallExternalApi() {
        IpLocationResponse response = service.find("UNKNOWN");

        assertThat(response.displayName()).isEqualTo("지역 확인 불가");
        verify(restTemplate, never()).getForObject(any(URI.class), org.mockito.ArgumentMatchers.eq(String.class));
    }
}
