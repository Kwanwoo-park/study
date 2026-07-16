package spring.study.common.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SessionManagerTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SessionManager sessionManager = new SessionManager(memberRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setLoginMemberShouldRegisterAdminAuthorityWithSpringSecurity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Member admin = Member.builder()
                .id(1L)
                .email("admin@example.com")
                .pwd("password")
                .name("admin")
                .role(Role.ADMIN)
                .phone("01000000000")
                .birth("20000101")
                .profile("profile.png")
                .build();

        sessionManager.setLoginMember(request, admin);

        MockHttpSession session = (MockHttpSession) request.getSession(false);
        SecurityContext savedContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );

        assertSame(admin, session.getAttribute("member"));
        assertSame(admin, savedContext.getAuthentication().getPrincipal());
        assertTrue(savedContext.getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(savedContext.getAuthentication(), SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logoutShouldClearMemberAndSpringSecurityAuthentication() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Member member = Member.builder()
                .id(2L)
                .email("user@example.com")
                .pwd("password")
                .name("user")
                .role(Role.USER)
                .phone("01000000001")
                .birth("20000101")
                .profile("profile.png")
                .build();
        sessionManager.setLoginMember(request, member);

        sessionManager.logout(request);

        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertNull(session.getAttribute("member"));
        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
