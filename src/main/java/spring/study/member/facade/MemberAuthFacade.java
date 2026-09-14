package spring.study.member.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.member.service.PasswordChangeVerificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberAuthFacade {
    private final MemberFacade memberFacade;
    private final MemberService memberService;
    private final JwtAuthenticationService jwtAuthenticationService;
    private final PasswordChangeVerificationService passwordChangeVerificationService;

    public ResponseEntity<?> logout(Member member, HttpServletRequest request, HttpServletResponse response) {
        jwtAuthenticationService.logout(request, response);

        return ResponseEntity.ok(Map.of(
                "result", member == null ? 0L : member.getId()
        ));
    }

    public ResponseEntity<?> recoverPassword(MemberRequestDto requestDto, Member loginMember) {
        if (loginMember != null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", -10L,
                    "message", "회원 설정의 비밀번호 변경 절차를 이용해주세요"
            ));
        }

        Member member = memberService.findMember(requestDto.getEmail());

        return memberFacade.updatePassword(requestDto.getPassword(), member);
    }

    public ResponseEntity<?> sendPasswordVerification(Member member) {
        passwordChangeVerificationService.sendCode(member);

        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "인증번호를 이메일로 발송했습니다"
        ));
    }

    public ResponseEntity<?> verifyPasswordChange(String code, Member member) {
        passwordChangeVerificationService.verifyCode(member, code);

        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "이메일 인증이 완료되었습니다"
        ));
    }

    public ResponseEntity<?> updateAuthenticatedPassword(String password, Member member,
                                                        HttpServletRequest request, HttpServletResponse response) {
        if (password == null || password.isBlank()) {
            return memberFacade.updatePassword(password, member);
        } else if (!passwordChangeVerificationService.consumeVerification(member)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", "이메일 인증 후 비밀번호를 변경할 수 있습니다"
            ));
        }

        ResponseEntity<?> result = memberFacade.updatePassword(password, member);
        if (result.getStatusCode().is2xxSuccessful()) {
            jwtAuthenticationService.logout(request, response);
        }

        return result;
    }

    public ResponseEntity<?> withdraw(Member member, HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity<?> result = memberFacade.deleteMember(member, request);
        jwtAuthenticationService.logout(request, response);

        return result;
    }
}
