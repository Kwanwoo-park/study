package spring.study.common.facade;

import spring.study.common.service.ModerationService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationFacade {
    private static final DateTimeFormatter SANCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ModerationService moderationService;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieService jwtCookieService;

    public int validate(String checkStr, Member member, HttpServletResponse response) {
        int risk = moderationService.evaluate(checkStr, member);
        return risk == 3 ? handleBlockRisk(member, response) : risk;
    }

    private int handleBlockRisk(Member member, HttpServletResponse response) {
        if (member.getRole() == Role.ADMIN) {
            notificationService.createNotification(
                    member,
                    "경고일자: " + LocalDateTime.now().format(SANCTION_DATE_FORMAT)
                            + " / 사유: 금칙어 사용 (관리자 계정 정지 제외)",
                    Group.ADMIN
            );
            moderationService.clear(member);
            return 1;
        }

        blockMember(member, response);
        return 3;
    }

    private void blockMember(Member member, HttpServletResponse response) {
        notificationService.createNotification(
                member,
                "금지일자: " + LocalDateTime.now().format(SANCTION_DATE_FORMAT) + " (영구) / 사유: 금칙어 사용",
                Group.ADMIN
        );
        notificationService.createNotification(
                memberService.findAdministrator(),
                member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                Group.ADMIN
        );

        memberService.updateRole(member.getId(), Role.DENIED);

        refreshTokenService.revokeAll(member.getId());
        jwtCookieService.clearAuthenticationCookies(response);
        SecurityContextHolder.clearContext();

        moderationService.clear(member);
    }
}
