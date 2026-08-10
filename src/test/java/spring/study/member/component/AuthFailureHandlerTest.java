package spring.study.member.component;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import spring.study.jwt.service.JwtCookieService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthFailureHandlerTest {
    @Test
    void mobileOAuthFailureReturnsToApp() throws Exception {
        JwtCookieService cookieService = mock(JwtCookieService.class);
        AuthFailureHandler handler = new AuthFailureHandler(cookieService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(cookieService.isMobileOAuth(request)).thenReturn(true);

        handler.onAuthenticationFailure(
                request, response, new BadCredentialsException("failed"));

        assertThat(response.getRedirectedUrl())
                .isEqualTo("kwanwooapp://oauth/callback?error=oauth_failed");
        verify(cookieService).clearMobileOAuthMarker(response);
    }
}
