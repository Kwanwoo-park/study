package spring.study.member.component;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MobileOAuthCodeService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthSuccessHandlerTest {
    @Test
    void webOAuthPassesTheCurrentBrowserToTokenReplacement() throws Exception {
        MemberService memberService = mock(MemberService.class);
        JwtAuthenticationService authenticationService = mock(JwtAuthenticationService.class);
        AuthSuccessHandler handler = new AuthSuccessHandler(memberService, authenticationService,
                mock(JwtCookieService.class), mock(MobileOAuthCodeService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        Member member = Member.builder().id(7L).email("oauth@test.com").build();
        when(authentication.getName()).thenReturn(member.getEmail());
        when(memberService.findMember(member.getEmail())).thenReturn(member);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authenticationService).login(member, request, response, "203.0.113.10");
        assertThat(response.getRedirectedUrl()).isEqualTo("/board/main");
    }

    @Test
    void mobileOAuthRedirectsWithExchangeCodeWithoutPuttingJwtInUrl() throws Exception {
        MemberService memberService = mock(MemberService.class);
        JwtAuthenticationService authenticationService = mock(JwtAuthenticationService.class);
        JwtCookieService cookieService = mock(JwtCookieService.class);
        MobileOAuthCodeService codeService = mock(MobileOAuthCodeService.class);
        AuthSuccessHandler handler = new AuthSuccessHandler(
                memberService, authenticationService, cookieService, codeService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = mock(Authentication.class);
        OAuth2User principal = mock(OAuth2User.class);
        Member member = Member.builder().id(7L).email("oauth@test.com").build();
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("email")).thenReturn("oauth@test.com");
        when(memberService.findMember("oauth@test.com")).thenReturn(member);
        when(cookieService.isMobileOAuth(request)).thenReturn(true);
        when(codeService.create(member)).thenReturn("one-time-code");

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("kwanwooapp://oauth/callback?code=one-time-code");
        verify(cookieService).clearMobileOAuthMarker(response);
        verify(authenticationService, never()).login(member, response);
    }
}
