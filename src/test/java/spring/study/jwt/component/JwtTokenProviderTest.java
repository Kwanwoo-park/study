package spring.study.jwt.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.common.exception.JwtValidationException;
import spring.study.jwt.entity.TokenName;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.jwt.dto.TokenClaims;

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
    void modifiedTokenShouldBeRejected() {
        String token = tokenProvider.createAccessToken(member).value();
        String[] parts = token.split("\\.");
        parts[1] = (parts[1].startsWith("a") ? "b" : "a") + parts[1].substring(1);
        String modified = String.join(".", parts);

        assertThrows(JwtValidationException.class, () -> tokenProvider.parse(modified, TokenName.ACCESS.getValue()));
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String token = tokenProvider.createRefreshToken(member).value();

        assertThrows(JwtValidationException.class, () -> tokenProvider.parse(token, TokenName.ACCESS.getValue()));
    }
}
