package spring.study.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.member.entity.Member;
import spring.study.jwt.dto.AuthenticationTokens;
import spring.study.jwt.dto.IssuedToken;
import spring.study.jwt.dto.TokenClaims;

import java.time.Duration;
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
        IssuedToken accessToken = tokenProvider.createAccessToken(member);
        IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
        refreshTokenService.save(refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress);
        return toAuthenticationTokens(accessToken, refreshToken);
    }

    public AuthenticationTokens refresh(String refreshTokenValue) {
        return refresh(refreshTokenValue, null);
    }

    public AuthenticationTokens refresh(String refreshTokenValue, String ipAddress) {
        TokenClaims claims = tokenProvider.parse(refreshTokenValue, JwtTokenProvider.REFRESH);
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

        IssuedToken accessToken = tokenProvider.createAccessToken(member);
        if (!tokenProvider.shouldRotateRefreshToken(claims.expiresAt())) {
            refreshTokenService.recordAccessIfNeeded(member, Duration.between(Instant.now(), claims.expiresAt()));
            return new AuthenticationTokens(accessToken.value(), refreshTokenValue, accessToken.expiresAt(), claims.expiresAt());
        }
        IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
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
            TokenClaims claims = tokenProvider.parse(
                    refreshTokenValue, JwtTokenProvider.REFRESH);
            refreshTokenService.revoke(claims.jti());
            return OptionalLong.of(claims.memberId());
        } catch (JwtTokenProvider.JwtValidationException ignored) {
            // Logout remains idempotent for expired or malformed client tokens.
            return OptionalLong.empty();
        }
    }

    private AuthenticationTokens toAuthenticationTokens(IssuedToken accessToken, IssuedToken refreshToken) {
        return new AuthenticationTokens(
                accessToken.value(),
                refreshToken.value(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }

}
