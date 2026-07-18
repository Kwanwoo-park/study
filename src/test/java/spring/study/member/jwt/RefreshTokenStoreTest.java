package spring.study.member.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RefreshTokenStore.class)
class RefreshTokenStoreTest {
    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void savedTokenIsValidOnlyForItsMember() {
        refreshTokenStore.save("valid-jti", 1L, Duration.ofDays(14));

        assertThat(refreshTokenStore.isValid("valid-jti", 1L)).isTrue();
        assertThat(refreshTokenStore.isValid("valid-jti", 2L)).isFalse();
    }

    @Test
    void expiredTokenIsInvalidAndCleanupDeletesIt() {
        refreshTokenStore.save("expired-jti", 1L, Duration.ofSeconds(-1));

        assertThat(refreshTokenStore.isValid("expired-jti", 1L)).isFalse();

        refreshTokenStore.deleteExpiredTokens();

        assertThat(refreshTokenRepository.existsById("expired-jti")).isFalse();
    }

    @Test
    void revokedTokenIsDeleted() {
        refreshTokenStore.save("revoked-jti", 1L, Duration.ofDays(14));

        refreshTokenStore.revoke("revoked-jti");

        assertThat(refreshTokenStore.isValid("revoked-jti", 1L)).isFalse();
    }
}
