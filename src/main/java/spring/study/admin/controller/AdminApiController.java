package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminApiController {
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;

    @PatchMapping("/member/permit")
    public ResponseEntity<Map<String, Integer>> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (member.getRole() != Role.ADMIN) {
            map.put("result", -10);
            session.invalidate();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }

        try {
            int result = memberService.updateRole(requestDto.getId(), Role.USER);
            map.put("result", result);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10);
        }

        return ResponseEntity.status(HttpStatus.OK).body(map);
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<Map<String, Integer>> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }

        try {
            int result = memberService.updateRole(requestDto.getId(), Role.USER);
            map.put("result", result);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10);
        }

        return ResponseEntity.status(HttpStatus.OK).body(map);
    }

    @GetMapping("/member/online")
    public ResponseEntity<Map<String, Object>> memberOnline(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        try {
            String count = redisTemplate.opsForValue().get("online:total");

            List<Long> userIds = redisTemplate.keys("online:user:*")
                    .stream()
                    .map(k -> k.substring(k.lastIndexOf(":") + 1))
                    .map(Long::parseLong)
                    .toList();

            List<Member> list = memberService.findMember(userIds);

            map.put("result", 1L);
            map.put("count", count != null ? Long.parseLong(count) : 0L);
            map.put("list", list);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("count", -1L);
            map.put("result", -1L);
        }

        return ResponseEntity.ok(map);
    }
}
