package spring.study.common.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SessionManagerTest {
    private final SessionManager sessionManager = new SessionManager();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getLoginMemberShouldUseJwtPopulatedSecurityContextWithoutCreatingSession() {
        Member member = createMember();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member, null, member.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertSame(member, sessionManager.getLoginMember(request));
        assertNull(request.getSession(false));
    }

    @Test
    void logoutShouldClearAuthenticationWithoutCreatingSession() {
        Member member = createMember();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(member, null, member.getAuthorities()));
        MockHttpServletRequest request = new MockHttpServletRequest();

        sessionManager.logout(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getSession(false));
    }

    private Member createMember() {
        return Member.builder()
                .id(1L).email("user@example.com").pwd("password").name("user")
                .role(Role.USER).phone("01000000000").birth("20000101").profile("profile.png")
                .build();
    }
}
