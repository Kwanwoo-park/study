package spring.study.jwt.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import spring.study.common.service.JwtManager;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.dto.MobileAuthResponse;
import spring.study.jwt.facade.MobileAuthFacade;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.jwt.dto.AuthenticationTokens;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

class MobileAuthControllerTest {
    @Test
    void loginReturnsMobileTokensAndSafeMemberProjection() {
        MemberService memberService = mock(MemberService.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        OnlineUserService onlineUserService = mock(OnlineUserService.class);
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        MemberTokenCacheService cacheService = mock(MemberTokenCacheService.class);
        RefreshTokenService tokenService = mock(RefreshTokenService.class);
        MobileAuthFacade facade = new MobileAuthFacade(provider, memberService, passwordEncoder, cacheService, onlineUserService, tokenService);
        MobileAuthController controller = new MobileAuthController(facade, mock(JwtManager.class));
        Member member = Member.builder()
                .id(3L).email("app@test.com").pwd("encoded-secret").name("app user")
                .role(Role.USER).phone("01000000000").birth("20000101").profile("profile.png")
                .build();
        MemberRequestDto request = MemberRequestDto.builder()
                .email("app@test.com").password("plain-secret").build();
        AuthenticationTokens tokens =
                new AuthenticationTokens(
                        "access", "refresh", Instant.now().plusSeconds(600), Instant.now().plusSeconds(1200));
        when(memberService.loadUserByUsername("app@test.com")).thenReturn(member);
        when(passwordEncoder.matches("plain-secret", "encoded-secret")).thenReturn(true);
        when(memberService.updateLastLoginTime(3L)).thenReturn(member);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("203.0.113.10");
        when(facade.issue(member, "203.0.113.10")).thenReturn(tokens);

        ResponseEntity<?> response = controller.login(request, servletRequest);

        assertEquals(200, response.getStatusCode().value());
        MobileAuthResponse body = assertInstanceOf(MobileAuthResponse.class, response.getBody());
        assertEquals("access", body.accessToken());
        assertEquals("refresh", body.refreshToken());
        assertEquals("app@test.com", body.member().email());
        verify(onlineUserService).markMobileActive(3L);
    }

    @Test
    void logoutShouldRemoveMobileOnlineState() {
        OnlineUserService onlineUserService = mock(OnlineUserService.class);
        JwtTokenProvider provider = mock(JwtTokenProvider.class);
        MemberTokenCacheService cacheService = mock(MemberTokenCacheService.class);
        RefreshTokenService tokenService = mock(RefreshTokenService.class);
        MobileAuthFacade facade = new MobileAuthFacade(provider, mock(MemberService.class), mock(BCryptPasswordEncoder.class), cacheService, onlineUserService, tokenService);
        MobileAuthController controller = new MobileAuthController(facade, mock(JwtManager.class));
        when(facade.revokeAndGetMemberId("refresh")).thenReturn(OptionalLong.of(3L));

        ResponseEntity<?> response = controller.logout(
                new spring.study.jwt.dto.MobileAuthRequest("refresh"));

        assertEquals(200, response.getStatusCode().value());
        verify(onlineUserService).markMobileInactive(3L);
    }
}
