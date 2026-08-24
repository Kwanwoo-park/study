package spring.study.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OnlineUserServiceTest {
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OnlineUserService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new OnlineUserService(redisTemplate);
    }

    @Test
    void webAndMobileUsersShouldBeCombinedWithoutDuplicates() {
        when(redisTemplate.keys("online:user:*")).thenReturn(Set.of(
                "online:user:1", "online:user:2", "online:user:invalid"));
        when(redisTemplate.keys("online:mobile:user:*")).thenReturn(Set.of(
                "online:mobile:user:2", "online:mobile:user:3"));

        Set<Long> onlineMembers = service.findOnlineMemberIds();

        assertEquals(Set.of(1L, 2L, 3L), onlineMembers);
    }

    @Test
    void bearerActivityShouldKeepMobileMemberOnlineForFiveMinutes() {
        service.markMobileActive(7L);

        verify(valueOperations).set(
                "online:mobile:user:7", "1", Duration.ofMinutes(5L));
    }

    @Test
    void totalShouldCountAMemberUsingWebAndMobileOnlyOnce() {
        when(redisTemplate.keys("online:user:*")).thenReturn(Set.of("online:user:1"));
        when(redisTemplate.keys("online:mobile:user:*")).thenReturn(Set.of(
                "online:mobile:user:1", "online:mobile:user:2"));

        service.syncOnlineTotal();

        verify(valueOperations).set("online:total", "2");
    }
}
