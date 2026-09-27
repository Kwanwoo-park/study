package spring.study.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final ForbiddenService forbiddenService;
    private final RedisTemplate<String, String> stringRedisTemplate;

    public int evaluate(String content, Member member) {
        if (content == null || content.isBlank()) return -99;
        int risk = forbiddenService.findWordList(Status.APPROVAL, content);
        if (risk == 3 || risk == 0) return risk;
        String key = key(member);
        stringRedisTemplate.opsForValue().setIfAbsent(key, "0", Duration.ofDays(1));
        Long count = stringRedisTemplate.opsForValue().increment(key, risk);
        return count != null && count >= 5 ? 3 : risk;
    }

    public void clear(Member member) { stringRedisTemplate.delete(key(member)); }

    private String key(Member member) { return "forbidden:user:" + member.getId(); }
}
