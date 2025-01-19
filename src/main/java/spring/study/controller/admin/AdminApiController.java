package spring.study.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.member.Role;
import spring.study.service.member.MemberService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final MemberService memberService;

    @PatchMapping("/member/permit")
    public ResponseEntity<Integer> memberPermit(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        int result = memberService.updateRole(requestDto.getId(), Role.USER);

        return ResponseEntity.status(200).body(result);
    }

    @PatchMapping("/member/deny")
    public ResponseEntity<Integer> memberDeny(@RequestBody MemberRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        int result = memberService.updateRole(requestDto.getId(), Role.DENIED);

        return ResponseEntity.status(200).body(result);
    }
}
