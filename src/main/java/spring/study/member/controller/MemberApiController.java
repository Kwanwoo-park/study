package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.PasswordVerificationRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.facade.MemberFacade;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.service.MemberService;
import spring.study.member.service.PasswordChangeVerificationService;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final MemberFacade memberFacade;
    private final MemberService memberService;
    private final JwtAuthenticationService jwtAuthenticationService;
    private final PasswordChangeVerificationService passwordChangeVerificationService;

    @PatchMapping("/login")
    public ResponseEntity<?> loginAction(@RequestBody MemberRequestDto dto, HttpServletResponse response) {
        return memberFacade.login(dto, response);
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutAction(HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
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
        Member member = jwtManager.getLoginMember(request);
        if (member != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "회원 설정의 비밀번호 변경 절차를 이용해주세요"
            ));
        }

        member = memberService.findMember(memberUpdateDto.getEmail());
        return memberFacade.updatePassword(memberUpdateDto.getPassword(), member);
    }

    @PostMapping("/password-verification/send")
    public ResponseEntity<?> sendPasswordVerification(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        passwordChangeVerificationService.sendCode(member);
        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "인증번호를 이메일로 발송했습니다"
        ));
    }

    @PostMapping("/password-verification/verify")
    public ResponseEntity<?> verifyPasswordChange(
            @Valid @RequestBody PasswordVerificationRequestDto verificationRequest,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().isEmpty()
                    ? "인증번호를 확인해주세요"
                    : bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of(
                    "result", -10L,
                    "message", message == null ? "인증번호를 확인해주세요" : message
            ));
        }

        passwordChangeVerificationService.verifyCode(member, verificationRequest.getCode());
        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "이메일 인증이 완료되었습니다"
        ));
    }

    @PatchMapping("/updatePassword/authenticated")
    public ResponseEntity<?> updateAuthenticatedPassword(
            @RequestBody MemberRequestDto memberUpdateDto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        if (memberUpdateDto.getPassword() == null || memberUpdateDto.getPassword().isBlank()) {
            return memberFacade.updatePassword(memberUpdateDto.getPassword(), member);
        }
        if (!passwordChangeVerificationService.consumeVerification(member)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", "이메일 인증 후 비밀번호를 변경할 수 있습니다"
            ));
        }

        ResponseEntity<?> result = memberFacade.updatePassword(memberUpdateDto.getPassword(), member);
        if (result.getStatusCode().is2xxSuccessful()) {
            jwtAuthenticationService.logout(request, response);
        }
        return result;
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

        ResponseEntity<?> result = memberFacade.deleteMember(member, request);
        jwtAuthenticationService.logout(request, response);
        return result;
    }
}
