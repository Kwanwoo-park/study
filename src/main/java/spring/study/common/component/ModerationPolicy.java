package spring.study.common.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.study.common.service.ModerationService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ModerationPolicy {
    private static final DateTimeFormatter SANCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ForbiddenService forbiddenService;
    private final ModerationService moderationService;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final MemberTokenCacheService tokenCacheService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public int validate(String content, Member member) {
        if (content == null || content.isBlank()) return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, content);
        if (risk == 0) return risk;
        if (risk != 3) risk = moderationService.evaluate(member, risk);
        if (risk != 3) return risk;

        String date = LocalDateTime.now().format(SANCTION_DATE_FORMAT);
        if (member.getRole() == Role.ADMIN) {
            notificationService.createNotification(
                    member, "경고일자: " + date + " / 사유: 금칙어 사용 (관리자 계정 정지 제외)", Group.ADMIN);
            moderationService.clear(member);
            return 1;
        }

        notificationService.createNotification(
                member, "금지일자: " + date + " (영구) / 사유: 금칙어 사용", Group.ADMIN);
        notificationService.createNotification(
                memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
        memberService.updateRole(member.getId(), Role.DENIED);
        refreshTokenService.revoke(member.getId());
        tokenCacheService.delete(member.getId());
        moderationService.clear(member);
        return 3;
    }
}
