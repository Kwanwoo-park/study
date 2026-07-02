package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.admin.facade.AdminFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.forbidden.dto.ForbiddenChangeRequestDto;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.facade.MemberFacade;
import spring.study.member.service.MemberService;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.entity.ReportStatus;
import spring.study.report.facade.ReportFacade;

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
    private final MemberFacade memberFacade;
    private final ForbiddenFacade forbiddenFacade;
    private final ReportFacade reportFacade;

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

    @DeleteMapping("/member/withdrawal")
    public ResponseEntity<?> memberWithdrawal(@RequestParam String email, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return memberFacade.deleteMember(email);
    }

    @PostMapping("/forbidden/word/save")
    public ResponseEntity<?> forbiddenWordSave(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return forbiddenFacade.wordSave(requestDto);
    }

    @PatchMapping("/forbidden/word/change/examine")
    public ResponseEntity<?> changeToExamine(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return forbiddenFacade.updateStatus(Status.EXAMINE, requestDto);
    }

    @PatchMapping("/forbidden/word/change/approval")
    public ResponseEntity<?> changeToApproval(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return forbiddenFacade.updateStatus(Status.APPROVAL, requestDto);
    }

    @GetMapping("/report")
    public ResponseEntity<?> findReports(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.findAll(ReportStatus.PENDING, 0, 10);
    }

    @GetMapping("/report/pending")
    public ResponseEntity<?> findPendingReports(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.findAllByStatus(ReportStatus.PENDING);
    }

    @GetMapping("/report/{id}")
    public ResponseEntity<?> findReport(@PathVariable Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.findById(id);
    }

    @PatchMapping("/report/{id}")
    public ResponseEntity<?> processReport(@PathVariable Long id,
                                           @Valid @RequestBody ReportProcessRequestDto requestDto,
                                           HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.process(id, requestDto);
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

    @GetMapping("/system/status")
    public ResponseEntity<?> systemStatus(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return adminFacade.systemStatus();
    }
}
