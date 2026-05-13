package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;

    public ResponseEntity<?> memberOnline() {
        String count = redisTemplate.opsForValue().get("online:total");

        List<Long> userIds = redisTemplate.keys("online:user:*")
                .stream()
                .map(k -> k.substring(k.lastIndexOf(":") + 1))
                .map(Long::parseLong)
                .toList();

        List<Member> list = memberService.findMember(userIds);

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count != null ? Long.parseLong(count) : 0L,
                "list", list
        ));
    }

    public ResponseEntity<?> newMember() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusDays(1);

        List<Member> list = memberService.findNewUser(start, end);
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }
}
