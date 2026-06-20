package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {
    private static final Duration ACTIVE_TTL = Duration.ofMinutes(2);

    private final RedisTemplate<String, String> redisTemplate;

    public void active(String roomId, Member member) {
        redisTemplate.opsForValue().set(createKey(roomId, member), "1", ACTIVE_TTL);
    }

    public void inactive(String roomId, Member member) {
        redisTemplate.delete(createKey(roomId, member));
    }

    public boolean isActive(String roomId, Member member) {
        Boolean hasKey = redisTemplate.hasKey(createKey(roomId, member));

        return Boolean.TRUE.equals(hasKey);
    }

    private String createKey(String roomId, Member member) {
        return "chat:room:" + roomId + ":active:" + member.getId();
    }
}
