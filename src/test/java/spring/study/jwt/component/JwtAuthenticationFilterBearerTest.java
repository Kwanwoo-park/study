package spring.study.jwt.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.common.service.OnlineUserService;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterBearerTest {
    private JwtTokenProvider tokenProvider;
    private Member member;
    private JwtCookieService cookieService;
    private MemberTokenCacheService memberTokenCacheService;
    private JwtAuthenticationFilter filter;
    private OnlineUserService onlineUserService;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(new ObjectMapper());
        ReflectionTestUtils.setField(tokenProvider, "configuredSecret",
                "test-secret-that-is-at-least-thirty-two-bytes-long");
        ReflectionTestUtils.setField(tokenProvider, "issuer", "study-test");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenMinutes", 15L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenDays", 14L);
        tokenProvider.initializeSecret();

        member = Member.builder()
                .id(9L).email("mobile@test.com").pwd("password").name("mobile")
                .role(Role.USER).phone("01000000000").birth("20000101").profile("profile.png")
                .build();
        cookieService = mock(JwtCookieService.class);
        memberTokenCacheService = mock(MemberTokenCacheService.class);
        onlineUserService = mock(OnlineUserService.class);
        filter = new JwtAuthenticationFilter(
                tokenProvider,
                cookieService,
                mock(RefreshTokenService.class),
                memberTokenCacheService,
                onlineUserService
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerAccessTokenAuthenticatesWithoutCookies() throws Exception {
        String accessToken = tokenProvider.createAccessToken(member).value();
        when(memberTokenCacheService.findOrLoad(eq(9L), any(Duration.class)))
                .thenReturn(Optional.of(member));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("mobile@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verifyNoInteractions(cookieService);
        verify(onlineUserService).markMobileActive(9L);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void cookieAccessTokenShouldNotBeRecordedAsMobileActivity() throws Exception {
        String accessToken = tokenProvider.createAccessToken(member).value();
        when(cookieService.read(any(), eq(JwtCookieService.ACCESS_COOKIE))).thenReturn(accessToken);
        when(memberTokenCacheService.findOrLoad(eq(9L), any(Duration.class)))
                .thenReturn(Optional.of(member));
        MockHttpServletRequest request = new MockHttpServletRequest();

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verifyNoInteractions(onlineUserService);
    }
}
