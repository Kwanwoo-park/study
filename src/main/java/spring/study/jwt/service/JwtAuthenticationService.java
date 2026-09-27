package spring.study.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.member.entity.Member;

import java.time.Instant;
import java.util.OptionalLong;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberTokenCacheService memberTokenCacheService;

    public AuthenticationTokens issue(Member member) {
        return issue(member, null);
    }

    public AuthenticationTokens issue(Member member, String ipAddress) {
        JwtTokenProvider.IssuedToken accessToken = tokenProvider.createAccessToken(member);
        JwtTokenProvider.IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenService.save(refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress);
        return toAuthenticationTokens(accessToken, refreshToken);
    }

    public AuthenticationTokens refresh(String refreshTokenValue) {
        return refresh(refreshTokenValue, null);
    }

    public AuthenticationTokens refresh(String refreshTokenValue, String ipAddress) {
        JwtTokenProvider.TokenClaims claims = tokenProvider.parse(refreshTokenValue, JwtTokenProvider.REFRESH);
        if (!refreshTokenService.isValid(claims.jti(), claims.memberId())) {
            throw new JwtTokenProvider.JwtValidationException("Revoked refresh token");
        }

        Member member = memberTokenCacheService
                .findOrLoad(claims.memberId(), tokenProvider.refreshTokenDuration())
                .orElseThrow(() -> new JwtTokenProvider.JwtValidationException("Member not found"));
        if (member.isAccessBlocked() || !member.getEmail().equals(claims.email())) {
            refreshTokenService.revoke(claims.jti());
            throw new JwtTokenProvider.JwtValidationException("Member access blocked");
        }

        JwtTokenProvider.IssuedToken accessToken = tokenProvider.createAccessToken(member);
        JwtTokenProvider.IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
        if (!refreshTokenService.rotate(
                claims.jti(), refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress)) {
            throw new JwtTokenProvider.JwtValidationException("Could not rotate refresh token");
        }
        return toAuthenticationTokens(accessToken, refreshToken);
    }

    public void revoke(String refreshTokenValue) {
        revokeAndGetMemberId(refreshTokenValue);
    }

    public OptionalLong revokeAndGetMemberId(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) return OptionalLong.empty();
        try {
            JwtTokenProvider.TokenClaims claims = tokenProvider.parse(
                    refreshTokenValue, JwtTokenProvider.REFRESH);
            refreshTokenService.revoke(claims.jti());
            return OptionalLong.of(claims.memberId());
        } catch (JwtTokenProvider.JwtValidationException ignored) {
            // Logout remains idempotent for expired or malformed client tokens.
            return OptionalLong.empty();
        }
    }

    private AuthenticationTokens toAuthenticationTokens(JwtTokenProvider.IssuedToken accessToken, JwtTokenProvider.IssuedToken refreshToken) {
        return new AuthenticationTokens(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }

    public record AuthenticationTokens(String accessToken, String refreshToken, Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {}
}
