package spring.study.jwt.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.entity.TokenName;
import spring.study.jwt.facade.JwtAuthenticationFacade;
import spring.study.jwt.repository.RefreshTokenRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.common.service.IpLocationService;
import spring.study.member.service.MemberService;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY,
        connection = EmbeddedDatabaseConnection.H2
)
@Import(RefreshTokenService.class)
class RefreshTokenServiceTest {
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberService memberService;

    @MockBean
    private MemberTokenCacheService memberTokenCacheService;

    @MockBean
    private IpLocationService ipLocationService;

    @Test
    void savedTokenIsValidOnlyForItsMember() {
        Member member = member(1L);
        Duration ttl = Duration.ofDays(14);
        refreshTokenService.save("valid-jti", member, ttl);

        assertThat(refreshTokenService.isValid("valid-jti", 1L)).isTrue();
        assertThat(refreshTokenService.isValid("valid-jti", 2L)).isFalse();
        assertThat(member.getLastLoginTime()).isNotNull();
        verify(memberTokenCacheService).save(member, ttl);
    }

    @Test
    void savedTokenKeepsLoginIpAddress() {
        Member member = member(1L);

        refreshTokenService.save("ip-jti", member, Duration.ofDays(14), "203.0.113.7");

        RefreshToken token = refreshTokenRepository.findById("ip-jti").orElseThrow();
        assertThat(token.getIpAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void revokeDeletesEveryTokenAndCacheForMember() {
        Member blockedMember = member(1L);
        Member otherMember = member(2L);
        refreshTokenService.save("blocked-first-jti", blockedMember, Duration.ofDays(14));
        refreshTokenService.save("blocked-second-jti", blockedMember, Duration.ofDays(14));
        refreshTokenService.save("other-jti", otherMember, Duration.ofDays(14));

        refreshTokenService.revoke(blockedMember.getId());

        assertThat(refreshTokenService.isValid("blocked-first-jti", blockedMember.getId())).isFalse();
        assertThat(refreshTokenService.isValid("blocked-second-jti", blockedMember.getId())).isFalse();
        assertThat(refreshTokenService.isValid("other-jti", otherMember.getId())).isTrue();
        verify(memberTokenCacheService).delete(blockedMember.getId());
    }

    @Test
    void logoutWithOnlyRefreshCookieMustNotLeaveANewRefreshTokenBehind() throws Exception {
        JwtTokenProvider provider = tokenProvider();
        Member member = member(1L);
        var refresh = provider.createRefreshToken(member);
        refreshTokenService.save(refresh.jti(), member, provider.refreshTokenDuration());
        when(memberTokenCacheService.findOrLoad(eq(member.getId()), any(Duration.class)))
                .thenReturn(Optional.of(member));
        JwtCookieService cookies = new JwtCookieService();
        JwtAuthenticationFacade authentication = new JwtAuthenticationFacade(provider, mock(MemberService.class), mock(MemberTokenCacheService.class), mock(RefreshTokenService.class), cookies);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, memberService, cookies, refreshTokenService, memberTokenCacheService, mock(OnlineUserService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/member/logout");
        request.setCookies(new Cookie(JwtCookieService.REFRESH_COOKIE, refresh.value()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> authentication.logout(request, response));

            assertThat(refreshTokenRepository.count()).isZero();
            assertThat(response.getHeaders("Set-Cookie")).allMatch(value -> value.contains("Max-Age=0"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void repeatedBrowserLoginReplacesOnlyItsTokenAndKeepsOtherDevices() {
        JwtTokenProvider provider = tokenProvider();
        Member member = member(1L);
        var browser = provider.createRefreshToken(member);
        var otherDevice = provider.createRefreshToken(member);
        refreshTokenService.save(browser.jti(), member, provider.refreshTokenDuration());
        refreshTokenService.save(otherDevice.jti(), member, provider.refreshTokenDuration());
        JwtCookieService cookies = new JwtCookieService();
        JwtAuthenticationFacade authentication = new JwtAuthenticationFacade(provider, mock(MemberService.class), mock(MemberTokenCacheService.class), mock(RefreshTokenService.class), cookies);
        String previousToken = browser.value();

        for (int attempt = 0; attempt < 3; attempt++) {
            MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/member/login");
            request.setCookies(new Cookie(JwtCookieService.REFRESH_COOKIE, previousToken));
            MockHttpServletResponse response = new MockHttpServletResponse();

            authentication.login(member, request, response, "203.0.113.10");

            String nextToken = response.getCookie(JwtCookieService.REFRESH_COOKIE).getValue();
            assertThat(nextToken).isNotEqualTo(previousToken);
            assertThat(refreshTokenService.isValid(provider.parse(previousToken, TokenName.REFRESH.getValue()).jti(), member.getId())).isFalse();
            assertThat(refreshTokenService.isValid(provider.parse(nextToken, TokenName.REFRESH.getValue()).jti(), member.getId())).isTrue();
            assertThat(refreshTokenService.isValid(otherDevice.jti(), member.getId())).isTrue();
            assertThat(refreshTokenRepository.count()).isEqualTo(2);
            verify(memberTokenCacheService, never()).delete(member.getId());
            previousToken = nextToken;
        }
    }

    @Test
    void refreshBeforeAnOperationThatLogsOutStillRevokesTheRotatedToken() throws Exception {
        JwtTokenProvider provider = tokenProvider();
        Member member = member(1L);
        ReflectionTestUtils.setField(provider, "refreshTokenDays", 1L);
        var refresh = provider.createRefreshToken(member);
        ReflectionTestUtils.setField(provider, "refreshTokenDays", 14L);
        refreshTokenService.save(refresh.jti(), member, provider.refreshTokenDuration());
        when(memberTokenCacheService.findOrLoad(eq(member.getId()), any(Duration.class)))
                .thenReturn(Optional.of(member));
        JwtCookieService cookies = new JwtCookieService();
        JwtAuthenticationFacade authentication = new JwtAuthenticationFacade(provider, mock(MemberService.class), mock(MemberTokenCacheService.class), mock(RefreshTokenService.class), cookies);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(provider, memberService, cookies, refreshTokenService, memberTokenCacheService, mock(OnlineUserService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/member/updatePassword/authenticated");
        request.setCookies(new Cookie(JwtCookieService.REFRESH_COOKIE, refresh.value()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (req, res) -> {
                assertThat(cookies.readCurrentRefreshToken(request)).isNotEqualTo(refresh.value());
                authentication.logout(request, response);
            });
            assertThat(refreshTokenRepository.count()).isZero();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private JwtTokenProvider tokenProvider() {
        JwtTokenProvider provider = new JwtTokenProvider(new ObjectMapper());
        ReflectionTestUtils.setField(provider, "configuredSecret", "test-secret-that-is-at-least-thirty-two-bytes-long");
        ReflectionTestUtils.setField(provider, "issuer", "study-test");
        ReflectionTestUtils.setField(provider, "accessTokenMinutes", 15L);
        ReflectionTestUtils.setField(provider, "refreshTokenDays", 14L);
        ReflectionTestUtils.invokeMethod(provider, "initializeSecret");
        return provider;
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .email("user@example.com")
                .name("user")
                .role(Role.USER)
                .phone("01000000000")
                .birth("20000101")
                .profile("profile.png")
                .build();
    }
}
