package spring.study.jwt.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationLifecycleTest {
    private JwtTokenProvider provider;
    private final JwtCookieService cookies = new JwtCookieService();
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final MemberTokenCacheService cache = mock(MemberTokenCacheService.class);
    private final Member member = Member.builder().id(9L).email("member@example.test").role(Role.USER).build();
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(new ObjectMapper());
        ReflectionTestUtils.setField(provider, "configuredSecret", "test-secret-that-is-at-least-thirty-two-bytes-long");
        ReflectionTestUtils.setField(provider, "issuer", "study-test");
        ReflectionTestUtils.setField(provider, "accessTokenMinutes", 15L);
        ReflectionTestUtils.setField(provider, "refreshTokenDays", 14L);
        provider.initializeSecret();
        provider = spy(provider);
        filter = new JwtAuthenticationFilter(provider, cookies, refreshTokens, cache, mock(OnlineUserService.class));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validAccessCookieDoesNotIssueOrRotateAnyToken() throws Exception {
        String access = provider.createAccessToken(member).value();
        clearInvocations(provider);
        when(cache.findOrLoad(eq(member.getId()), any(Duration.class))).thenReturn(Optional.of(member));
        var request = new MockHttpServletRequest("GET", "/board/main");
        request.setCookies(new Cookie(JwtCookieService.ACCESS_COOKIE, access));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(member);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        verify(provider, never()).createAccessToken(any());
        verify(provider, never()).createRefreshToken(any());
        verifyNoInteractions(refreshTokens);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/member/login", "/api/member/logout",
            "/api/mobile/auth/login", "/api/mobile/auth/refresh", "/api/mobile/auth/logout",
            "/api/mobile/auth/oauth/exchange", "/login/oauth2/code/google", "/login/oauth2/code/naver"
    })
    void tokenManagementDoesNotAutomaticallyRotateBeforeItsOwnHandler(String path) throws Exception {
        var refresh = provider.createRefreshToken(member);
        clearInvocations(provider);
        when(refreshTokens.isValid(refresh.jti(), member.getId())).thenReturn(true);
        when(cache.findOrLoad(eq(member.getId()), any(Duration.class))).thenReturn(Optional.of(member));
        var request = new MockHttpServletRequest("POST", "/study" + path);
        request.setContextPath("/study");
        request.setCookies(new Cookie(JwtCookieService.REFRESH_COOKIE, refresh.value()));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(member);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
        verify(provider, never()).createAccessToken(any());
        verify(provider, never()).createRefreshToken(any());
        verify(refreshTokens, never()).rotate(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void expiredAccessCookieStillRotatesOnceOnAnOrdinaryAuthenticatedRequest() throws Exception {
        ReflectionTestUtils.setField(provider, "accessTokenMinutes", -1L);
        String expiredAccess = provider.createAccessToken(member).value();
        ReflectionTestUtils.setField(provider, "accessTokenMinutes", 15L);
        var refresh = provider.createRefreshToken(member);
        clearInvocations(provider);
        when(refreshTokens.isValid(refresh.jti(), member.getId())).thenReturn(true);
        when(cache.findOrLoad(eq(member.getId()), any(Duration.class))).thenReturn(Optional.of(member));
        when(refreshTokens.rotate(eq(refresh.jti()), anyString(), eq(member), any(Duration.class), anyString())).thenReturn(true);
        var request = new MockHttpServletRequest("GET", "/board/main");
        request.setCookies(new Cookie(JwtCookieService.ACCESS_COOKIE, expiredAccess),
                new Cookie(JwtCookieService.REFRESH_COOKIE, refresh.value()));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
        assertThat(cookies.readCurrentRefreshToken(request)).isEqualTo(response.getCookie(JwtCookieService.REFRESH_COOKIE).getValue());
        assertThat(cookies.readCurrentRefreshToken(request)).isNotEqualTo(refresh.value());
        verify(provider).createAccessToken(member);
        verify(provider).createRefreshToken(member);
    }

    @Test
    void revokedRefreshCookieCannotAuthenticateEvenOnLogout() throws Exception {
        var refresh = provider.createRefreshToken(member);
        clearInvocations(provider);
        var request = new MockHttpServletRequest("GET", "/api/member/logout");
        request.setCookies(new Cookie(JwtCookieService.REFRESH_COOKIE, refresh.value()));

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(cache);
        verify(provider, never()).createRefreshToken(any());
    }
}
