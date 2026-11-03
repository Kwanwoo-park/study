package spring.study.jwt.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.common.exception.JwtValidationException;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.dto.AuthenticationTokens;
import spring.study.jwt.dto.IssuedToken;
import spring.study.jwt.dto.TokenClaims;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.entity.TokenName;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationFacade {
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final MemberTokenCacheService memberTokenCacheService;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieService cookieService;

    public void login(Member member, HttpServletResponse response) { login(member, null, response, null); }

    public void login(Member member, HttpServletResponse response, String ipAddress) { login(member, null, response, ipAddress); }

    public void login(Member member, HttpServletRequest request, HttpServletResponse response, String ipAddress) {
        AuthenticationTokens tokens = issue(member, ipAddress);
        if (request != null) {
            revokeAndGetMemberId(cookieService.readCurrentRefreshToken(request));
            cookieService.rememberRefreshToken(request, tokens.refreshToken());
        }
        cookieService.writeAccessToken(response, tokens.accessToken(), tokenProvider.accessTokenDuration());
        cookieService.writeRefreshToken(response, tokens.refreshToken(), tokenProvider.refreshTokenDuration());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        revokeAndGetMemberId(cookieService.readCurrentRefreshToken(request));
        cookieService.clearAuthenticationCookies(response);
        SecurityContextHolder.clearContext();
    }

    private AuthenticationTokens issue(Member member, String ipAddress) {
        IssuedToken accessToken = tokenProvider.createAccessToken(member);
        IssuedToken refreshToken = tokenProvider.createRefreshToken(member);

        memberService.updateLastLoginTime(member.getId());
        refreshTokenService.save(refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress);
        memberTokenCacheService.save(member, tokenProvider.refreshTokenDuration());

        return toAuthenticationTokens(accessToken, refreshToken);
    }

    private AuthenticationTokens toAuthenticationTokens(IssuedToken accessToken, IssuedToken refreshToken) {
        return new AuthenticationTokens(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }

    private void revokeAndGetMemberId(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank())
            return;
        try {
            TokenClaims claims = tokenProvider.parse(refreshTokenValue, TokenName.REFRESH.getValue());
            RefreshToken oldToken = refreshTokenService.findOldToken(claims.jti());

            if (oldToken != null) {
                refreshTokenService.deleteOldToken(oldToken);

                if (!refreshTokenService.existsByMemberIdAndExpiresAtAfter(oldToken.getMemberId(), Instant.now()))
                    memberTokenCacheService.delete(oldToken.getMemberId());
            }
        } catch (JwtValidationException ignored) {
            // Logout remains idempotent for expired or malformed client tokens.
        }
    }
}
