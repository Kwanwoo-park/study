package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Role;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final MemberService memberService;
    private final ForbiddenService forbiddenService;

    @PatchMapping("/member/permit")
    public ResponseEntity<Integer> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        int result = memberService.updateRole(requestDto.getId(), Role.USER);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<Integer> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        int result = memberService.updateRole(requestDto.getId(), Role.DENIED);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
