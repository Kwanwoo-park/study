package spring.study.common.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private static final DateTimeFormatter SANCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieService jwtCookieService;

    public int validate(String checkStr, Member member, HttpServletResponse response) {
        if (checkStr == null || checkStr.isBlank()) return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, checkStr);
        String key = "forbidden:user:" + member.getId();

        if (risk == 3) {
            risk = handleBlockRisk(member, response, key);
        } else if (risk != 0) {
            stringRedisTemplate.opsForValue().setIfAbsent(key, "0", Duration.ofDays(1));

            Long count = stringRedisTemplate.opsForValue().increment(key, risk);

            if (count != null && count >= 5) {
                risk = handleBlockRisk(member, response, key);
            }
        }

        return risk;
    }

    private int handleBlockRisk(Member member, HttpServletResponse response, String key) {
        if (member.getRole() == Role.ADMIN) {
            notificationService.createNotification(
                    member,
                    "경고일자: " + LocalDateTime.now().format(SANCTION_DATE_FORMAT)
                            + " / 사유: 금칙어 사용 (관리자 계정 정지 제외)",
                    Group.ADMIN
            );
            stringRedisTemplate.delete(key);
            return 1;
        }

        blockMember(member, response, key);
        return 3;
    }

    private void blockMember(Member member, HttpServletResponse response, String key) {
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

        stringRedisTemplate.delete(key);
    }
}
