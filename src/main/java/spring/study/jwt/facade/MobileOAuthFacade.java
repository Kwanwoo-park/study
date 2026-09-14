package spring.study.jwt.facade;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.dto.MobileOAuthExchangeRequest;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MobileOAuthCodeService;
import spring.study.member.service.MemberService;
import spring.study.common.service.OnlineUserService;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MobileOAuthFacade {
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "naver");

    private final JwtCookieService cookieService;
    private final MobileOAuthCodeService codeService;
    private final MemberService memberService;
    private final JwtAuthenticationService authenticationService;
    private final OnlineUserService onlineUserService;

    public ResponseEntity<?> start(String provider, HttpServletResponse response) {
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            return error(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다");
        }
        cookieService.writeMobileOAuthMarker(response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/" + provider))
                .build();
    }

    public ResponseEntity<?> exchange(MobileOAuthExchangeRequest request, String ipAddress) {
        return codeService.consume(request.code())
                .map(memberService::updateLastLoginTime)
                .<ResponseEntity<?>>map(member -> {
                    JwtAuthenticationService.AuthenticationTokens tokens =
                            authenticationService.issue(member, ipAddress);
                    onlineUserService.markMobileActive(member.getId());
                    return ResponseEntity.ok(new MobileAuthResponse(member, tokens));
                })
                .orElseGet(() -> error(HttpStatus.UNAUTHORIZED, "OAuth 로그인이 만료되었거나 유효하지 않습니다"));
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", -1, "message", message));
    }
}
