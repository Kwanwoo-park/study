package spring.study.jwt.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.dto.MobileOAuthExchangeRequest;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MobileOAuthCodeService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobileOAuthControllerTest {
    private final JwtCookieService cookieService = mock(JwtCookieService.class);
    private final MobileOAuthCodeService codeService = mock(MobileOAuthCodeService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final JwtAuthenticationService authenticationService = mock(JwtAuthenticationService.class);
    private final MobileOAuthController controller = new MobileOAuthController(
            cookieService, codeService, memberService, authenticationService);

    @Test
    void startsSupportedProviderThroughServerAuthorizationEndpoint() {
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        ResponseEntity<?> response = controller.start("naver", servletResponse);

        assertEquals(302, response.getStatusCode().value());
        assertThat(response.getHeaders().getLocation().toString())
                .isEqualTo("/oauth2/authorization/naver");
        verify(cookieService).writeMobileOAuthMarker(servletResponse);
    }

    @Test
    void rejectsUnknownProvider() {
        assertEquals(400, controller.start("unknown", mock(HttpServletResponse.class))
                .getStatusCode().value());
    }

    @Test
    void exchangesOneTimeCodeForMobileTokens() {
        Member member = Member.builder().id(7L).email("oauth@test.com").name("oauth")
                .role(Role.USER).build();
        JwtAuthenticationService.AuthenticationTokens tokens =
                new JwtAuthenticationService.AuthenticationTokens(
                        "access", "refresh", Instant.now().plusSeconds(600), Instant.now().plusSeconds(1200));
        when(codeService.consume("once")).thenReturn(Optional.of(7L));
        when(memberService.updateLastLoginTime(7L)).thenReturn(member);
        when(authenticationService.issue(member)).thenReturn(tokens);

        ResponseEntity<?> response = controller.exchange(new MobileOAuthExchangeRequest("once"));

        assertEquals(200, response.getStatusCode().value());
        MobileAuthResponse body = assertInstanceOf(MobileAuthResponse.class, response.getBody());
        assertEquals("access", body.accessToken());
        assertEquals("oauth@test.com", body.member().email());
    }

    @Test
    void rejectsConsumedOrExpiredCode() {
        when(codeService.consume("expired")).thenReturn(Optional.empty());

        assertEquals(401, controller.exchange(new MobileOAuthExchangeRequest("expired"))
                .getStatusCode().value());
    }
}
