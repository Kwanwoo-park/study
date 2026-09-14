package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.common.service.ClientIpResolver;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.PasswordVerificationRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.member.facade.MemberAuthFacade;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final MemberFacade memberFacade;
    private final MemberAuthFacade memberAuthFacade;

    @PatchMapping("/login")
    public ResponseEntity<?> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request, HttpServletResponse response) {
        return memberFacade.login(dto, response, ClientIpResolver.resolve(request));
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutAction(HttpServletRequest request, HttpServletResponse response) {
        return memberAuthFacade.logout(jwtManager.getLoginMember(request), request, response);
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
        Member member = jwtManager.getLoginMember(request);
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
    public ResponseEntity<?> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request, HttpServletResponse response) {
        return memberAuthFacade.recoverPassword(memberUpdateDto, jwtManager.getLoginMember(request));
    }

    @PostMapping("/password-verification/send")
    public ResponseEntity<?> sendPasswordVerification(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return memberAuthFacade.sendPasswordVerification(member);
    }

    @PostMapping("/password-verification/verify")
    public ResponseEntity<?> verifyPasswordChange(
            @Valid @RequestBody PasswordVerificationRequestDto verificationRequest,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "인증번호를 확인해주세요");

        return memberAuthFacade.verifyPasswordChange(verificationRequest.getCode(), member);
    }

    @PatchMapping("/updatePassword/authenticated")
    public ResponseEntity<?> updateAuthenticatedPassword(
            @RequestBody MemberRequestDto memberUpdateDto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return memberAuthFacade.updateAuthenticatedPassword(memberUpdateDto.getPassword(), member, request, response);
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<?> updatePhone(@RequestBody @Valid MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        return memberFacade.updatePhone(memberUpdateDto, request);
    }

    @PatchMapping("/visibility")
    public ResponseEntity<?> updateVisibility(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return memberFacade.updateVisibility(memberUpdateDto.getVisibility(), member);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMember(@RequestParam() String name, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return memberFacade.search(name, member);
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<?> withdrawalAction(HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return memberAuthFacade.withdraw(member, request, response);
    }
}
