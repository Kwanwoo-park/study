package spring.study.jwt.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final JwtCookieService cookieService;
    private final RefreshTokenService refreshTokenService;
    private final MemberTokenCacheService memberTokenCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Member member = authenticateAccessToken(request);
            if (member == null) member = refreshAuthentication(request, response);
            if (member != null) setAuthentication(member);
        }
        filterChain.doFilter(request, response);
    }

    private Member authenticateAccessToken(HttpServletRequest request) {
        String token = cookieService.read(request, JwtCookieService.ACCESS_COOKIE);
        if (token == null) return null;
        try {
            return loadActiveMember(tokenProvider.parse(token, JwtTokenProvider.ACCESS));
        } catch (JwtTokenProvider.JwtValidationException ignored) {
            return null;
        }
    }

    private Member refreshAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieService.read(request, JwtCookieService.REFRESH_COOKIE);
        if (token == null) return null;
        try {
            JwtTokenProvider.TokenClaims claims = tokenProvider.parse(token, JwtTokenProvider.REFRESH);
            if (!refreshTokenService.isValid(claims.jti(), claims.memberId())) return null;
            Member member = loadActiveMember(claims);
            if (member == null) {
                refreshTokenService.revoke(claims.jti());
                cookieService.clearAuthenticationCookies(response);
                return null;
            }

            JwtTokenProvider.IssuedToken accessToken = tokenProvider.createAccessToken(member);
            JwtTokenProvider.IssuedToken refreshToken = tokenProvider.createRefreshToken(member);
            if (!refreshTokenService.rotate(
                    claims.jti(), refreshToken.jti(), member, tokenProvider.refreshTokenDuration())) {
                cookieService.clearAuthenticationCookies(response);
                return null;
            }
            cookieService.writeAccessToken(response, accessToken.value(), tokenProvider.accessTokenDuration());
            cookieService.writeRefreshToken(response, refreshToken.value(), tokenProvider.refreshTokenDuration());
            return member;
        } catch (JwtTokenProvider.JwtValidationException ignored) {
            cookieService.clearAuthenticationCookies(response);
            return null;
        }
    }

    private Member loadActiveMember(JwtTokenProvider.TokenClaims claims) {
        Member member = memberTokenCacheService
                .findOrLoad(claims.memberId(), tokenProvider.refreshTokenDuration())
                .orElse(null);
        if (member == null || member.isAccessBlocked() || !member.getEmail().equals(claims.email())) return null;
        return member;
    }

    private void setAuthentication(Member member) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                member, null, member.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
