package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;

    @PatchMapping("/member/permit")
    public ResponseEntity<Integer> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        int result = memberService.updateRole(requestDto.getId(), Role.USER);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<Integer> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        int result = memberService.updateRole(requestDto.getId(), Role.DENIED);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/member/online")
    public ResponseEntity<Map<String, Object>> memberOnline(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Map<String, Object> map = new HashMap<>();

        String count = redisTemplate.opsForValue().get("online:total");

        List<Long> userIds = redisTemplate.keys("online:user:*")
                .stream()
                .map(k -> k.substring(k.lastIndexOf(":") + 1))
                .map(Long::parseLong)
                .toList();

        List<Member> list = memberService.findMember(userIds);

        map.put("count", count != null ? Long.parseLong(count) : 0L);
        map.put("list", list);

        return ResponseEntity.ok(map);
    }
}
