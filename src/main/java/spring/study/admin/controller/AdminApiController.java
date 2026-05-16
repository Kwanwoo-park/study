package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.admin.facade.AdminFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminApiController {
    private final MemberService memberService;
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final AdminFacade adminFacade;

    @PatchMapping("/member/permit")
    public ResponseEntity<?> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(requestDto.getId(), Role.USER)
        ));
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<?> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return ResponseEntity.ok(Map.of(
                "result", memberService.updateRole(requestDto.getId(), Role.DENIED)
        ));
    }

    @GetMapping("/member/online")
    public ResponseEntity<?> memberOnline(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return adminFacade.memberOnline();
    }

    @GetMapping("/member/new")
    public ResponseEntity<?> newMember(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return adminFacade.newMember();
    }

    @GetMapping("/board/new")
    public ResponseEntity<?> newBoard(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return adminFacade.newBoard();
    }

    @GetMapping("/chat/active")
    public ResponseEntity<?> chattingActive(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return adminFacade.chattingActive();
    }
}
