package spring.study.jwt.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.ClientIpResolver;
import spring.study.common.service.JwtManager;
import spring.study.jwt.dto.MobileAuthRequest;
import spring.study.jwt.facade.MobileAuthFacade;
import spring.study.member.dto.MemberRequestDto;

@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
public class MobileAuthController {
    private final MobileAuthFacade mobileAuthFacade;
    private final JwtManager jwtManager;

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        return mobileAuthFacade.me(jwtManager.getLoginMember(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody MemberRequestDto request, HttpServletRequest servletRequest) {
        return mobileAuthFacade.login(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody MobileAuthRequest request, HttpServletRequest servletRequest) {
        return mobileAuthFacade.refresh(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody MobileAuthRequest request) {
        return mobileAuthFacade.logout(request);
    }
}
