package spring.study.jwt.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.member.entity.Member;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationFacade {
    private final JwtAuthenticationService authenticationService;
    private final JwtCookieService cookieService;
    private final JwtTokenProvider tokenProvider;

    public void login(Member member, HttpServletResponse response) { login(member, null, response, null); }

    public void login(Member member, HttpServletResponse response, String ipAddress) { login(member, null, response, ipAddress); }

    public void login(Member member, HttpServletRequest request, HttpServletResponse response, String ipAddress) {
        var tokens = authenticationService.issue(member, ipAddress);
        if (request != null) {
            authenticationService.revoke(cookieService.readCurrentRefreshToken(request));
            cookieService.rememberRefreshToken(request, tokens.refreshToken());
        }
        cookieService.writeAccessToken(response, tokens.accessToken(), tokenProvider.accessTokenDuration());
        cookieService.writeRefreshToken(response, tokens.refreshToken(), tokenProvider.refreshTokenDuration());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authenticationService.revoke(cookieService.readCurrentRefreshToken(request));
        cookieService.clearAuthenticationCookies(response);
        SecurityContextHolder.clearContext();
    }
}
