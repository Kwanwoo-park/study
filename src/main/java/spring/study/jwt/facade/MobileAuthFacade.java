package spring.study.jwt.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import spring.study.common.exception.JwtValidationException;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.component.RefreshTokenRotation;
import spring.study.jwt.dto.*;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.entity.TokenName;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.component.PasswordLoginGuard;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.OptionalLong;

@Service
@RequiredArgsConstructor
public class MobileAuthFacade {
    private final JwtTokenProvider tokenProvider;
    private final MemberService memberService;
    private final PasswordLoginGuard passwordLoginGuard;
    private final MemberTokenCacheService memberTokenCacheService;
    private final OnlineUserService onlineUserService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRotation rotation;

    public ResponseEntity<?> me(Member member) {
        if (member == null) return error(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        return ResponseEntity.ok(new MobileMemberResponse(member));
    }

    public ResponseEntity<?> login(MemberRequestDto request, String ipAddress) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "이메일과 비밀번호를 입력해주세요");
        }

        try {
            Member member = passwordLoginGuard.authenticate(request.getEmail(), request.getPassword(), ipAddress);
            member = memberService.updateLastLoginTime(member.getId());
            AuthenticationTokens tokens = issue(member, ipAddress);
            onlineUserService.markMobileActive(member.getId());
            return ResponseEntity.ok(new MobileAuthResponse(member, tokens));
        } catch (BadCredentialsException exception) {
            return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
        } catch (DisabledException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        }
    }

    public ResponseEntity<?> refresh(MobileAuthRequest request, String ipAddress) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다");
        }
        try {
            AuthenticationTokens tokens = refresh(request.refreshToken(), ipAddress);
            return ResponseEntity.ok(Map.of(
                    "result", 1,
                    "accessToken", tokens.accessToken(),
                    "refreshToken", tokens.refreshToken(),
                    "accessTokenExpiresAt", tokens.accessTokenExpiresAt(),
                    "refreshTokenExpiresAt", tokens.refreshTokenExpiresAt()
            ));
        } catch (JwtValidationException exception) {
            return error(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다");
        }
    }

    public ResponseEntity<?> logout(MobileAuthRequest request) {
        revokeAndGetMemberId(request.refreshToken()).ifPresent(onlineUserService::markMobileInactive);

        return ResponseEntity.ok(Map.of("result", 1));
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", -1, "message", message));
    }

    public AuthenticationTokens issue(Member member, String ipAddress) {
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

    public OptionalLong revokeAndGetMemberId(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) return OptionalLong.empty();
        try {
            TokenClaims claims = tokenProvider.parse(refreshTokenValue, TokenName.REFRESH.getValue());
            RefreshToken oldToken = refreshTokenService.findOldToken(claims.jti());

            if (oldToken != null) {
                refreshTokenService.deleteOldToken(oldToken);

                if (!refreshTokenService.existsByMemberIdAndExpiresAtAfter(oldToken.getMemberId(), Instant.now()))
                    memberTokenCacheService.delete(oldToken.getMemberId());
            }

            return OptionalLong.of(claims.memberId());
        } catch (JwtValidationException ignored) {
            // Logout remains idempotent for expired or malformed client tokens.
            return OptionalLong.empty();
        }
    }

    private AuthenticationTokens refresh(String refreshTokenValue, String ipAddress) {
        TokenClaims claims = tokenProvider.parse(refreshTokenValue, TokenName.REFRESH.getValue());
        if (!refreshTokenService.isValid(claims.jti(), claims.memberId())) {
            throw new JwtValidationException("Revoked refresh token");
        }

        Member member = memberTokenCacheService
                .findOrLoad(claims.memberId(), tokenProvider.refreshTokenDuration())
                .orElseThrow(() -> new JwtValidationException("Member not found"));
        if (member.isAccessBlocked() || !member.getEmail().equals(claims.email())) {
            RefreshToken oldToken = refreshTokenService.findOldToken(claims.jti());

            if (oldToken != null) {
                refreshTokenService.deleteOldToken(oldToken);

                if (!refreshTokenService.existsByMemberIdAndExpiresAtAfter(oldToken.getMemberId(), Instant.now()))
                    memberTokenCacheService.delete(oldToken.getMemberId());
            }

            throw new JwtValidationException("Member access blocked");
        }

        IssuedToken accessToken = tokenProvider.createAccessToken(member);
        if (!tokenProvider.shouldRotateRefreshToken(claims.expiresAt())) {
            if (member.getLastLoginTime() == null || !member.getLastLoginTime().isAfter(LocalDateTime.now().minusDays(1))) {
                memberService.updateLastLoginTime(member.getId());
                memberTokenCacheService.save(member, Duration.between(Instant.now(), claims.expiresAt()));
            }

            return new AuthenticationTokens(accessToken.value(), refreshTokenValue, accessToken.expiresAt(), claims.expiresAt());
        }
        IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
        if (!rotation.rotate(
                claims.jti(), refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress)) {
            throw new JwtValidationException("Could not rotate refresh token");
        }
        return toAuthenticationTokens(accessToken, refreshToken);
    }

}
