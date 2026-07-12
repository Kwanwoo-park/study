package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    @InjectMocks
    private ModerationService moderationService;

    @Test
    void validateShouldNotifyBlockedMemberWithDateAndReason() {
        Member member = createMember(1L, "member@test.com", Role.USER);
        Member admin = createMember(2L, "admin@test.com", Role.ADMIN);

        when(forbiddenService.findWordList(Status.APPROVAL, "blocked word")).thenReturn(3);
        when(memberService.findAdministrator()).thenReturn(admin);
        when(request.getSession(false)).thenReturn(session);

        int result = moderationService.validate("blocked word", member, request);

        verify(notificationService).createNotification(
                eq(member),
                argThat(message -> message.contains("금지일자:") && message.contains("사유: 금칙어 사용")),
                eq(Group.ADMIN)
        );
        verify(memberService).updateRole(member.getId(), Role.DENIED);
        verify(session).invalidate();
        verify(stringRedisTemplate).delete("forbidden:user:" + member.getId());
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(3);
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
