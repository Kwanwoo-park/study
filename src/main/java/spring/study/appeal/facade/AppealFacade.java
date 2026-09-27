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

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import spring.study.member.service.MemberService;
import spring.study.member.sanction.service.MemberSanctionService;
import spring.study.notification.service.NotificationService;
import spring.study.notification.entity.Group;
import spring.study.common.component.TransactionRunner;
import spring.study.appeal.dto.AppealSanctionResponseDto;
import spring.study.appeal.entity.Appeal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppealFacade {
    private final AppealService appealService;
    private final AppealVerificationService appealVerificationService;
    private final MemberService memberService;
    private final MemberSanctionService memberSanctionService;
    private final NotificationService notificationService;
    private final TransactionRunner transactions;

    @Transactional(readOnly = true)
    public ResponseEntity<?> context(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "memberName", member.getName() == null ? "" : member.getName(),
                "memberEmail", member.getEmail(),
                "sanctions", memberSanctionService.findByMember(member).stream().map(AppealSanctionResponseDto::new).toList(),
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
            AppealResponseDto appeal = submit(requestDto, member);

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

    public AppealResponseDto submit(AppealRequestDto requestDto, Member authenticatedMember) {
        return transactions.write(() -> {
            Member member = resolveMember(requestDto, authenticatedMember);
            var sanction = memberSanctionService.findOwned(requestDto.getSanctionId(), member);
            Appeal appeal = appealService.create(requestDto, member, sanction);
            notifyAdministrators(appeal);
            return new AppealResponseDto(appeal);
        });
    }

    @Transactional
    public AppealResponseDto acceptAndUnblock(Long id) {
        Appeal appeal = appealService.accept(id);
        memberSanctionService.unblock(appeal.getMember(), appeal.getRelatedSanction());
        notificationService.createNotification(appeal.getMember(), "상소가 받아들여져 계정 차단이 해제되었습니다.", Group.ADMIN);
        return new AppealResponseDto(appeal);
    }

    private Member resolveMember(AppealRequestDto requestDto, Member authenticatedMember) {
        if (authenticatedMember != null) {
            return authenticatedMember;
        }
        if (requestDto.getEmail() == null || requestDto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비로그인 상소는 이메일 인증이 필요합니다");
        }
        return appealVerificationService.consumeVerification(
                requestDto.getEmail(), requestDto.getVerificationToken());
    }

    private void notifyAdministrators(Appeal appeal) {
        String memberName = appeal.getMember().getName() == null || appeal.getMember().getName().isBlank()
                ? appeal.getMember().getEmail()
                : appeal.getMember().getName();
        String message = memberName + "님이 상소문을 제출했습니다.";
        String url = "/admin/appeal?appealId=" + appeal.getId();

        memberService.findAdministrators().forEach(admin ->
                notificationService.createNotification(admin, message, Group.ADMIN, url)
        );
    }
}
