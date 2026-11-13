package spring.study.common.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import spring.study.common.service.ModerationService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationFacadeTest {
    @Mock private ForbiddenService forbiddenService;
    @Mock private ModerationService moderationService;
    @Mock private NotificationService notificationService;
    @Mock private MemberService memberService;
    @Mock private MemberTokenCacheService tokenCacheService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtCookieService jwtCookieService;
    @InjectMocks private ModerationFacade moderationFacade;

    @Test
    void accumulatedRiskBlocksMemberAndInvalidatesAuthentication() {
        Member member = Member.builder().id(1L).name("member").role(Role.USER).build();
        Member administrator = Member.builder().id(2L).role(Role.ADMIN).build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(forbiddenService.findWordList(Status.APPROVAL, "금칙어")).thenReturn(2);
        when(moderationService.evaluate(member, 2)).thenReturn(3);
        when(memberService.findAdministrator()).thenReturn(administrator);

        assertThat(moderationFacade.validate("금칙어", member, response)).isEqualTo(3);

        verify(moderationService).evaluate(member, 2);
        verify(memberService).updateRole(member.getId(), Role.DENIED);
        verify(refreshTokenService).revoke(member.getId());
        verify(tokenCacheService).delete(member.getId());
        verify(jwtCookieService).clearAuthenticationCookies(response);
        verify(notificationService).createNotification(eq(member), contains("금지일자"), eq(Group.ADMIN));
        verify(notificationService).createNotification(eq(administrator), contains("차단"), eq(Group.ADMIN));
        verify(moderationService).clear(member);
    }

    @Test
    void administratorReceivesWarningInsteadOfBeingBlocked() {
        Member administrator = Member.builder().id(2L).role(Role.ADMIN).build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(forbiddenService.findWordList(Status.APPROVAL, "금칙어")).thenReturn(3);

        assertThat(moderationFacade.validate("금칙어", administrator, response)).isEqualTo(1);

        verify(notificationService).createNotification(eq(administrator), contains("관리자 계정 정지 제외"), eq(Group.ADMIN));
        verify(moderationService).clear(administrator);
        verifyNoInteractions(memberService, refreshTokenService, tokenCacheService, jwtCookieService);
    }
}
