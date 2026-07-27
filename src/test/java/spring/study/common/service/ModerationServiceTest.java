package spring.study.common.service;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {
    @Mock
    private ForbiddenService forbiddenService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MemberService memberService;
    @Mock
    private RedisTemplate<String, String> stringRedisTemplate;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtCookieService jwtCookieService;

    @InjectMocks
    private ModerationService moderationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validateShouldNotifyBlockedMemberWithDateAndReason() {
        Member member = createMember(1L, "member@test.com", Role.USER);
        Member admin = createMember(2L, "admin@test.com", Role.ADMIN);

        when(forbiddenService.findWordList(Status.APPROVAL, "blocked word")).thenReturn(3);
        when(memberService.findAdministrator()).thenReturn(admin);

        int result = moderationService.validate("blocked word", member, response);

        verify(notificationService).createNotification(
                eq(member),
                argThat(message -> message.contains("금지일자:") && message.contains("사유: 금칙어 사용")),
                eq(Group.ADMIN)
        );
        verify(memberService).updateRole(member.getId(), Role.DENIED);
        verify(refreshTokenService).revokeAll(member.getId());
        verify(jwtCookieService).clearAuthenticationCookies(response);
        verify(stringRedisTemplate).delete("forbidden:user:" + member.getId());
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(3);
    }

    @Test
    void validateShouldWarnAdministratorWithoutBlockingAuthentication() {
        Member admin = createMember(2L, "admin@test.com", Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities())
        );

        when(forbiddenService.findWordList(Status.APPROVAL, "blocked word")).thenReturn(3);

        int result = moderationService.validate("blocked word", admin, response);

        verify(notificationService).createNotification(
                eq(admin),
                argThat(message -> message.contains("경고일자:")
                        && message.contains("사유: 금칙어 사용")
                        && message.contains("관리자 계정 정지 제외")),
                eq(Group.ADMIN)
        );
        verify(memberService, never()).updateRole(admin.getId(), Role.DENIED);
        verify(refreshTokenService, never()).revokeAll(admin.getId());
        verify(jwtCookieService, never()).clearAuthenticationCookies(response);
        verify(stringRedisTemplate).delete("forbidden:user:" + admin.getId());
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        ).isSameAs(admin);
    }

    private Member createMember(Long id, String email, Role role) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("pwd")
                .name("member")
                .role(role)
                .phone("010-0000-000" + id)
                .birth("2000-01-01")
                .profile("profile")
                .build();
    }
}
