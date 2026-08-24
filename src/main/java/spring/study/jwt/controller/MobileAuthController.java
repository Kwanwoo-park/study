package spring.study.jwt.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.dto.MobileAuthRequest;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.dto.MobileMemberResponse;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.common.service.JwtManager;
import spring.study.common.service.OnlineUserService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {
    private final MemberService memberService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtAuthenticationService authenticationService;
    private final JwtManager jwtManager;
    private final OnlineUserService onlineUserService;

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return error(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        return ResponseEntity.ok(new MobileMemberResponse(member));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberRequestDto request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "이메일과 비밀번호를 입력해주세요");
        }

        try {
            Member member = (Member) memberService.loadUserByUsername(request.getEmail());
            if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
                return error(HttpStatus.UNAUTHORIZED, "이메일이나 비밀번호를 확인해주세요");
            }
            if (member.isAccessBlocked()) {
                return error(HttpStatus.FORBIDDEN, "차단된 계정입니다");
            }

            member = memberService.updateLastLoginTime(member.getId());
            JwtAuthenticationService.AuthenticationTokens tokens = authenticationService.issue(member);
            onlineUserService.markMobileActive(member.getId());
            return ResponseEntity.ok(new MobileAuthResponse(member, tokens));
        } catch (BadCredentialsException exception) {
            return error(HttpStatus.UNAUTHORIZED, "이메일이나 비밀번호를 확인해주세요");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody MobileAuthRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다");
        }
        try {
            JwtAuthenticationService.AuthenticationTokens tokens = authenticationService.refresh(request.refreshToken());
            return ResponseEntity.ok(Map.of(
                    "result", 1,
                    "accessToken", tokens.accessToken(),
                    "refreshToken", tokens.refreshToken(),
                    "accessTokenExpiresAt", tokens.accessTokenExpiresAt(),
                    "refreshTokenExpiresAt", tokens.refreshTokenExpiresAt()
            ));
        } catch (JwtTokenProvider.JwtValidationException exception) {
            return error(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody MobileAuthRequest request) {
        authenticationService.revokeAndGetMemberId(request.refreshToken())
                .ifPresent(onlineUserService::markMobileInactive);
        return ResponseEntity.ok(Map.of("result", 1));
    }

    private ResponseEntity<?> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", -1, "message", message));
    }
}
