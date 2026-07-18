package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.service.MemberService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final MemberFacade memberFacade;
    private final MemberService memberService;
    private final JwtAuthenticationService jwtAuthenticationService;

    @PatchMapping("/login")
    public ResponseEntity<?> loginAction(@RequestBody MemberRequestDto dto, HttpServletResponse response) {
        return memberFacade.login(dto, response);
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutAction(HttpServletRequest request, HttpServletResponse response) {
        Member member = sessionManager.getLoginMember(request);
        jwtAuthenticationService.logout(request, response);

        return ResponseEntity.ok(Map.of(
                "result", member == null ? 0L : member.getId()
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
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

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
    public ResponseEntity<?> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        } else {
            jwtAuthenticationService.logout(request, response);
        }

        return memberFacade.updatePassword(memberUpdateDto.getPassword(), member);
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<?> updatePhone(@RequestBody @Valid MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        return memberFacade.updatePhone(memberUpdateDto, request);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMember(@RequestParam() String name, HttpServletRequest request) {
        if (sessionManager.getLoginMember(request) == null) return commonFacade.unauthorized();

        return memberFacade.search(name);
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<?> withdrawalAction(HttpServletRequest request, HttpServletResponse response) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> result = memberFacade.deleteMember(member, request);
        jwtAuthenticationService.logout(request, response);
        return result;
    }
}
