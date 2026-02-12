package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.admin.facade.AdminFacade;
import spring.study.common.service.SessionService;
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
    private final SessionService sessionService;
    private final AdminFacade adminFacade;

    @PatchMapping("/member/permit")
    public ResponseEntity<?> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10L,
                    "message", "잘못된 접근입니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(requestDto.getId(), Role.USER)
        ));
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<?> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10L,
                    "message", "잘못된 접근입니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(requestDto.getId(), Role.DENIED)
        ));
    }

    @GetMapping("/member/online")
    public ResponseEntity<?> memberOnline(HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10L,
                    "message", "잘못된 접근입니다"
            ));
        }

        return adminFacade.memberOnline();
    }
}
