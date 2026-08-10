package spring.study.jwt.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.dto.MobileOAuthExchangeRequest;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MobileOAuthCodeService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/mobile/auth/oauth")
@RequiredArgsConstructor
public class MobileOAuthController {
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "naver");

    private final JwtCookieService cookieService;
    private final MobileOAuthCodeService codeService;
    private final MemberService memberService;
    private final JwtAuthenticationService authenticationService;

    @GetMapping("/{provider}")
    public ResponseEntity<?> start(@PathVariable String provider, HttpServletResponse response) {
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            return error(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다");
        }
        cookieService.writeMobileOAuthMarker(response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/" + provider))
                .build();
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchange(@RequestBody MobileOAuthExchangeRequest request) {
        return codeService.consume(request.code())
                .map(memberService::updateLastLoginTime)
                .<ResponseEntity<?>>map(member -> ResponseEntity.ok(
                        new MobileAuthResponse(member, authenticationService.issue(member))))
                .orElseGet(() -> error(HttpStatus.UNAUTHORIZED, "OAuth 로그인이 만료되었거나 유효하지 않습니다"));
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", -1, "message", message));
    }
}
