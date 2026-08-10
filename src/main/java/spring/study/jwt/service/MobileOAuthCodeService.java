package spring.study.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MobileOAuthCodeService {
    private static final String KEY_PREFIX = "auth:mobile-oauth:";
    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    private final RedisTemplate<String, String> redisTemplate;

    public String create(Member member) {
        String code = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_PREFIX + code, member.getId().toString(), CODE_TTL);
        return code;
    }

    public Optional<Long> consume(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String memberId = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + code);
        if (memberId == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(memberId));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
