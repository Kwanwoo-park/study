package spring.study.appeal.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.dto.AppealVerificationConfirmRequest;
import spring.study.appeal.service.AppealService;
import spring.study.appeal.service.AppealVerificationService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppealFacade {
    private final AppealService appealService;
    private final AppealVerificationService appealVerificationService;

    public ResponseEntity<?> context(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "memberName", member.getName() == null ? "" : member.getName(),
                "memberEmail", member.getEmail(),
                "sanctions", appealService.findSanctions(member),
                "appeals", appealService.findByMember(member)
        ));
    }

    public ResponseEntity<?> sendVerificationCode(String email) {
        try {
            appealVerificationService.sendCode(email);

            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "expiresInSeconds", 300,
                    "message", "가입된 이메일이라면 인증번호가 발송됩니다. 인증번호는 5분 동안 유효합니다"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "인증번호를 발송할 수 없습니다");
        }
    }

    public ResponseEntity<?> verifyEmail(AppealVerificationConfirmRequest request) {
        try {
            String verificationToken = appealVerificationService.verifyCode(request.email(), request.code());

            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "verificationToken", verificationToken,
                    "expiresInSeconds", 300,
                    "message", "이메일 인증이 완료되었습니다. 5분 이내에 상소문을 제출해주세요"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "이메일 인증에 실패했습니다");
        }
    }

    public ResponseEntity<?> create(AppealRequestDto requestDto, Member member) {
        try {
            AppealResponseDto appeal = appealService.create(requestDto, member);

            return ResponseEntity.ok(Map.of(
                    "result", appeal.getId(),
                    "appeal", appeal,
                    "message", "상소문이 접수되었습니다"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "상소문을 접수할 수 없습니다");
        }
    }

    private ResponseEntity<?> statusFailure(ResponseStatusException exception, String fallbackMessage) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "result", -10L,
                "message", exception.getReason() == null ? fallbackMessage : exception.getReason()
        ));
    }
}
