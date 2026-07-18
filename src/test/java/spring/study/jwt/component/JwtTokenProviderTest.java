package spring.study.jwt.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {
    private JwtTokenProvider tokenProvider;
    private Member member;

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
                .id(7L).email("jwt@test.com").pwd("password").name("jwt")
                .role(Role.ADMIN).phone("01000000000").birth("20000101").profile("profile.png")
                .build();
    }

    @Test
    void accessTokenShouldContainAuthenticatedMemberClaims() {
        String token = tokenProvider.createAccessToken(member).value();

        JwtTokenProvider.TokenClaims claims = tokenProvider.parse(token, JwtTokenProvider.ACCESS);

        assertEquals(7L, claims.memberId());
        assertEquals("jwt@test.com", claims.email());
        assertEquals("ROLE_ADMIN", claims.role());
    }

    @Test
    void modifiedTokenShouldBeRejected() {
        String token = tokenProvider.createAccessToken(member).value();
        String[] parts = token.split("\\.");
        parts[1] = (parts[1].startsWith("a") ? "b" : "a") + parts[1].substring(1);
        String modified = String.join(".", parts);

        assertThrows(JwtTokenProvider.JwtValidationException.class,
                () -> tokenProvider.parse(modified, JwtTokenProvider.ACCESS));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String token = tokenProvider.createRefreshToken(member).value();

        assertThrows(JwtTokenProvider.JwtValidationException.class,
                () -> tokenProvider.parse(token, JwtTokenProvider.ACCESS));
    }
}
