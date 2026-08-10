package spring.study.jwt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import spring.study.common.service.JwtManager;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class MobileAuthControllerTest {
    @Test
    void loginReturnsMobileTokensAndSafeMemberProjection() {
        MemberService memberService = mock(MemberService.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        JwtAuthenticationService authenticationService = mock(JwtAuthenticationService.class);
        MobileAuthController controller = new MobileAuthController(
                memberService, passwordEncoder, authenticationService, mock(JwtManager.class));
        Member member = Member.builder()
                .id(3L).email("app@test.com").pwd("encoded-secret").name("app user")
                .role(Role.USER).phone("01000000000").birth("20000101").profile("profile.png")
                .build();
        MemberRequestDto request = MemberRequestDto.builder()
                .email("app@test.com").password("plain-secret").build();
        JwtAuthenticationService.AuthenticationTokens tokens =
                new JwtAuthenticationService.AuthenticationTokens(
                        "access", "refresh", Instant.now().plusSeconds(600), Instant.now().plusSeconds(1200));
        when(memberService.loadUserByUsername("app@test.com")).thenReturn(member);
        when(passwordEncoder.matches("plain-secret", "encoded-secret")).thenReturn(true);
        when(memberService.updateLastLoginTime(3L)).thenReturn(member);
        when(authenticationService.issue(member)).thenReturn(tokens);

        ResponseEntity<?> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        MobileAuthResponse body = assertInstanceOf(MobileAuthResponse.class, response.getBody());
        assertEquals("access", body.accessToken());
        assertEquals("refresh", body.refreshToken());
        assertEquals("app@test.com", body.member().email());
    }
}
