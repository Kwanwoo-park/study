package spring.study.forbidden.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.SessionService;
import spring.study.forbidden.dto.ForbiddenChangeRequestDto;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/forbidden/word")
public class ForbiddenApiController {
    private final SessionService sessionService;
    private final ForbiddenFacade forbiddenFacade;

    @GetMapping("/search")
    public ResponseEntity<?> forbiddenWordSearch(@RequestParam String word, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return forbiddenFacade.search(word);
    }

    @GetMapping("/proposal")
    public ResponseEntity<?> forbiddenProposalWordSearch(HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return forbiddenFacade.getStatus(Status.PROPOSAL);
    }

    @GetMapping("/examine")
    public ResponseEntity<?> forbiddenExamineWordSearch(HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return forbiddenFacade.getStatus(Status.EXAMINE);
    }

    @GetMapping("/approval")
    public ResponseEntity<?> forbiddenApprovalWordSearch(HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return forbiddenFacade.getStatus(Status.APPROVAL);
    }

    @PostMapping("/apply")
    public ResponseEntity<?> forbiddenWordApply(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return forbiddenFacade.wordApply(requestDto);
    }

    @PostMapping("/admin/save")
    public ResponseEntity<?> forbiddenWordSave(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (!member.getRole().equals(Role.ADMIN)) {
            request.getSession(false).invalidate();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "비정상적인 접근입니다"
            ));
        }

        return forbiddenFacade.wordSave(requestDto);
    }

    @PatchMapping("/admin/change/examine")
    public ResponseEntity<?> changeToExamine(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (!member.getRole().equals(Role.ADMIN)) {
            request.getSession(false).invalidate();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10,
                    "message", "비정상적인 접근입니다"
            ));
        }

        return forbiddenFacade.updateStatus(Status.EXAMINE, requestDto);
    }

    @PatchMapping("/admin/change/approval")
    public ResponseEntity<?> changeToApproval(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        if (!member.getRole().equals(Role.ADMIN)) {
            request.getSession(false).invalidate();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10,
                    "message", "비정상적인 접근입니다"
            ));
        }

        return forbiddenFacade.updateStatus(Status.APPROVAL, requestDto);
    }
}
