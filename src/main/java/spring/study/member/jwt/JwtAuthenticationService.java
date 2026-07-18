package spring.study.member.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {
    private final JwtTokenProvider tokenProvider;
    private final JwtCookieService cookieService;
    private final RefreshTokenStore refreshTokenStore;

    public void login(Member member, HttpServletResponse response) {
        JwtTokenProvider.IssuedToken accessToken = tokenProvider.createAccessToken(member);
        JwtTokenProvider.IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenStore.save(refreshToken.jti(), member.getId(), tokenProvider.refreshTokenDuration());
        cookieService.writeAccessToken(response, accessToken.value(), tokenProvider.accessTokenDuration());
        cookieService.writeRefreshToken(response, refreshToken.value(), tokenProvider.refreshTokenDuration());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.read(request, JwtCookieService.REFRESH_COOKIE);
        if (refreshToken != null) {
            try {
                refreshTokenStore.revoke(tokenProvider.parse(refreshToken, JwtTokenProvider.REFRESH).jti());
            } catch (JwtTokenProvider.JwtValidationException ignored) {
                // Invalid client tokens still result in cookie cleanup.
            }
        }
        cookieService.clearAuthenticationCookies(response);
        SecurityContextHolder.clearContext();
    }
}
