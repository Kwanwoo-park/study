package spring.study.member.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {
    private static final String PREFIX = "auth:refresh:";
    private final RedisTemplate<String, String> redisTemplate;

    public void save(String jti, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + jti, memberId.toString(), ttl);
    }

    public boolean isValid(String jti, Long memberId) {
        return memberId.toString().equals(redisTemplate.opsForValue().get(PREFIX + jti));
    }

    public void revoke(String jti) {
        redisTemplate.delete(PREFIX + jti);
    }
}
