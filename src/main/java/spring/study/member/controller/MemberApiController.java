package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.common.service.SessionService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberFacade;
import spring.study.member.service.MemberService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final SessionService sessionService;
    private final MemberFacade memberFacade;
    private final MemberService memberService;

    @PatchMapping("/login")
    public ResponseEntity<?> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request) {
        return memberFacade.login(dto, request);
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutAction(HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        sessionService.logout(request, memberService.getIp(request));

        return ResponseEntity.ok(Map.of(
                "result", member.getId()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAction(@RequestBody @Valid MemberRequestDto memberRequestDto) throws Exception {
        return memberFacade.register(memberRequestDto);
    }

    @GetMapping("/duplicateCheck")
    public ResponseEntity<?> duplicateCheck(@RequestParam() String email) {
        return memberFacade.duplicateCheck(email);
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<?> detailAction(@RequestPart MultipartFile file, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10L,
                    "message", "유효하지 않은 세션입니다"
            ));
        }

        return memberFacade.changeProfileImage(file, member, request);
    }

    @GetMapping("/find/email")
    public ResponseEntity<?> findAction(@RequestParam() String email) {
        return memberFacade.findEmail(email);
    }

    @GetMapping("/find/info")
    public ResponseEntity<?> findAction(@RequestParam String birth, @RequestParam String phone) {
        return memberFacade.findInfo(birth, phone);
    }

    @PatchMapping("/updatePassword")
    public ResponseEntity<?> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        } else {
            request.getSession(false).invalidate();
        }

        return memberFacade.updatePassword(memberUpdateDto.getPassword(), member);
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<?> updatePhone(@RequestBody @Valid MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        return memberFacade.updatePhone(memberUpdateDto, request);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMember(@RequestParam() String name, HttpServletRequest request) {
        if (sessionService.getLoginMember(request) == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return memberFacade.search(name);
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<?> withdrawalAction(@RequestParam(required = false) String email, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10L,
                    "message", "유효하지 않은 세션입니다"
            ));
        }

        if (email == null) return memberFacade.deleteMember(member, request);
        else return memberFacade.deleteMember(email);
    }
}
