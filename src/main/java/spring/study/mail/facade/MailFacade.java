package spring.study.mail.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class MailFacade {
    private final EmailVerificationService verificationService;
    private final MemberService memberService;

    public ResponseEntity<HashMap<String, String>> confirm(String email) {
        verificationService.send(EmailVerificationPurpose.EMAIL, email, email);
        HashMap<String, String> result = new HashMap<>();
        result.put("status", "ok");
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<HashMap<String, String>> verify(String email, String code) {
        String token = verificationService.verify(EmailVerificationPurpose.EMAIL, email, code);
        HashMap<String, String> result = new HashMap<>();
        result.put("verificationToken", token);
        result.put("status", "ok");
        return ResponseEntity.ok(result);
    }

    public boolean consumeEmailVerification(String email, String token) {
        return verificationService.consume(EmailVerificationPurpose.EMAIL, email, token);
    }

    public void sendAppealCode(String email) {
        Member member = findAppealMemberOrNull(email);
        if (member != null) {
            verificationService.send(EmailVerificationPurpose.APPEAL, member.getId().toString(), member.getEmail());
        }
    }

    public String verifyAppealCode(String email, String code) {
        Member member = findAppealMember(email);
        return verificationService.verify(EmailVerificationPurpose.APPEAL, member.getId().toString(), code);
    }

    public Member consumeAppealVerification(String email, String token) {
        if (token == null || token.isBlank()) {
            throw appealVerificationRequired();
        }

        Member member = findAppealMember(email);
        if (!verificationService.consume(EmailVerificationPurpose.APPEAL, member.getId().toString(), token)) {
            throw appealVerificationRequired();
        }
        return member;
    }

    public void sendPasswordCode(Member member) {
        verificationService.send(EmailVerificationPurpose.PASSWORD_CHANGE, member.getId().toString(), member.getEmail());
    }

    public void verifyPasswordCode(Member member, String code) {
        verificationService.verifyPasswordChange(member.getId().toString(), code);
    }

    public boolean consumePasswordVerification(Member member) {
        return verificationService.consumePasswordChange(member.getId().toString());
    }

    private Member findAppealMember(String email) {
        Member member = findAppealMemberOrNull(email);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 올바르지 않습니다");
        }
        return member;
    }

    private Member findAppealMemberOrNull(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return memberService.findMemberOrNull(email.trim());
    }

    private ResponseStatusException appealVerificationRequired() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "5분 이내에 이메일 인증을 완료한 후 상소문을 제출해주세요");
    }
}
