package spring.study.appeal.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.appeal.dto.AppealRequestDto;
import spring.study.appeal.dto.AppealResponseDto;
import spring.study.appeal.dto.AppealVerificationConfirmRequest;
import spring.study.appeal.service.AppealService;
import spring.study.mail.service.EmailVerificationService;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.member.entity.Member;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import spring.study.sanction.entity.MemberSanction;
import spring.study.member.service.MemberService;
import spring.study.sanction.service.MemberSanctionService;
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
    private final EmailVerificationService verificationService;
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
            Member member = findAppealMemberOrNull(email);
            if (member != null) {
                verificationService.send(EmailVerificationPurpose.APPEAL, member.getId().toString(), member.getEmail());
            }

            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "expiresInSeconds", EmailVerificationService.CODE_TTL.toSeconds(),
                    "message", "가입된 이메일이라면 인증번호가 발송됩니다. 인증번호는 5분 동안 유효합니다"
            ));
        } catch (ResponseStatusException exception) {
            return statusFailure(exception, "인증번호를 발송할 수 없습니다");
        }
    }

    public ResponseEntity<?> verifyEmail(AppealVerificationConfirmRequest request) {
        try {
            Member member = findAppealMember(request.email());
            String verificationToken = verificationService.verify(
                    EmailVerificationPurpose.APPEAL, member.getId().toString(), member.getEmail(), request.code());

            return ResponseEntity.ok(Map.of(
                    "result", 1L,
                    "verificationToken", verificationToken,
                    "message", "이메일 인증이 완료되었습니다. 시간 제한 없이 상소문을 작성할 수 있어요"
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

    private AppealResponseDto submit(AppealRequestDto requestDto, Member authenticatedMember) {
        return transactions.write(() -> {
            Member member = resolveMember(requestDto, authenticatedMember);
            MemberSanction sanction = memberSanctionService.findOwned(requestDto.getSanctionId(), member);
            Appeal appeal = appealService.create(requestDto, member, sanction);
            notifyAdministrators(appeal);
            return new AppealResponseDto(appeal);
        });
    }

    private Member resolveMember(AppealRequestDto requestDto, Member authenticatedMember) {
        if (authenticatedMember != null) {
            return authenticatedMember;
        }

        if (requestDto.getEmail() == null || requestDto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비로그인 상소는 이메일 인증이 필요합니다");
        }

        String token = requestDto.getVerificationToken();

        if (token == null || token.isBlank()) {
            throw appealVerificationRequired();
        }

        Member member = findAppealMember(requestDto.getEmail());

        if (!verificationService.consume(EmailVerificationPurpose.APPEAL, member.getId().toString(), member.getEmail(), token)) {
            throw appealVerificationRequired();
        }

        return member;
    }

    private void notifyAdministrators(Appeal appeal) {
        String message = appeal.getMember().displayName() + "님이 상소문을 제출했습니다.";
        String url = "/admin/appeal?appealId=" + appeal.getId();

        memberService.findAdministrators().forEach(admin ->
                notificationService.createNotification(admin, message, Group.ADMIN, url)
        );
    }

    private Member findAppealMemberOrNull(String email) {
        return email == null || email.isBlank() ? null : memberService.findMemberOrNull(email.trim());
    }

    private Member findAppealMember(String email) {
        Member member = findAppealMemberOrNull(email);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 올바르지 않습니다");
        }
        return member;
    }

    private ResponseStatusException appealVerificationRequired() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "이메일 인증을 완료한 후 상소문을 제출해주세요");
    }

}
