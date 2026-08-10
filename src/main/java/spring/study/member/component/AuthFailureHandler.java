package spring.study.member.component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import spring.study.jwt.service.JwtCookieService;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final JwtCookieService cookieService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (cookieService.isMobileOAuth(request)) {
            cookieService.clearMobileOAuthMarker(response);
            getRedirectStrategy().sendRedirect(request, response,
                    "kwanwooapp://oauth/callback?error=oauth_failed");
            return;
        }
        String msg = "Invalid Email or Password";

        if (exception instanceof DisabledException) {
            msg = "DisabledException account";
        } else if (exception instanceof CredentialsExpiredException) {
            msg = "CredentialExpiredException account";
        } else if (exception instanceof BadCredentialsException) {
            msg = "BadCredentialsException account";
        }

        setDefaultFailureUrl("/login?error=true&exception=" + msg);

        super.onAuthenticationFailure(request, response, exception);
    }
}
