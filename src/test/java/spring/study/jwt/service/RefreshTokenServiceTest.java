package spring.study.jwt.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import spring.study.jwt.repository.RefreshTokenRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(RefreshTokenService.class)
class RefreshTokenServiceTest {
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private MemberTokenCacheService memberTokenCacheService;

    @Test
    void savedTokenIsValidOnlyForItsMember() {
        Member member = member(1L);
        Duration ttl = Duration.ofDays(14);
        refreshTokenService.save("valid-jti", member, ttl);

        assertThat(refreshTokenService.isValid("valid-jti", 1L)).isTrue();
        assertThat(refreshTokenService.isValid("valid-jti", 2L)).isFalse();
        verify(memberTokenCacheService).save(member, ttl);
    }

    @Test
    void expiredTokenIsInvalidAndCleanupDeletesIt() {
        refreshTokenService.save("expired-jti", member(1L), Duration.ofSeconds(-1));

        assertThat(refreshTokenService.isValid("expired-jti", 1L)).isFalse();

        refreshTokenService.deleteExpiredTokens();

        assertThat(refreshTokenRepository.existsById("expired-jti")).isFalse();
        verify(memberTokenCacheService).delete(1L);
    }

    @Test
    void revokedTokenIsDeleted() {
        refreshTokenService.save("revoked-jti", member(1L), Duration.ofDays(14));

        refreshTokenService.revoke("revoked-jti");

        assertThat(refreshTokenService.isValid("revoked-jti", 1L)).isFalse();
        verify(memberTokenCacheService).delete(1L);
    }

    @Test
    void revokingOneTokenKeepsCacheWhenMemberHasAnotherValidToken() {
        Member member = member(1L);
        refreshTokenService.save("first-jti", member, Duration.ofDays(14));
        refreshTokenService.save("second-jti", member, Duration.ofDays(14));

        refreshTokenService.revoke("first-jti");

        assertThat(refreshTokenService.isValid("first-jti", 1L)).isFalse();
        assertThat(refreshTokenService.isValid("second-jti", 1L)).isTrue();
        verify(memberTokenCacheService, never()).delete(1L);
    }

    @Test
    void revokeAllDeletesEveryTokenAndCacheForMember() {
        Member blockedMember = member(1L);
        Member otherMember = member(2L);
        refreshTokenService.save("blocked-first-jti", blockedMember, Duration.ofDays(14));
        refreshTokenService.save("blocked-second-jti", blockedMember, Duration.ofDays(14));
        refreshTokenService.save("other-jti", otherMember, Duration.ofDays(14));

        refreshTokenService.revokeAll(blockedMember.getId());

        assertThat(refreshTokenService.isValid("blocked-first-jti", blockedMember.getId())).isFalse();
        assertThat(refreshTokenService.isValid("blocked-second-jti", blockedMember.getId())).isFalse();
        assertThat(refreshTokenService.isValid("other-jti", otherMember.getId())).isTrue();
        verify(memberTokenCacheService).delete(blockedMember.getId());
    }

    @Test
    void cleanupKeepsCacheWhenMemberHasANewerValidToken() {
        Member member = member(1L);
        refreshTokenService.save("expired-jti", member, Duration.ofSeconds(-1));
        refreshTokenService.save("valid-jti", member, Duration.ofDays(14));

        refreshTokenService.deleteExpiredTokens();

        assertThat(refreshTokenRepository.existsById("expired-jti")).isFalse();
        assertThat(refreshTokenService.isValid("valid-jti", 1L)).isTrue();
        verify(memberTokenCacheService, never()).delete(1L);
    }

    @Test
    void rotationReplacesTokenWithoutDeletingMemberCache() {
        Member member = member(1L);
        Duration ttl = Duration.ofDays(14);
        refreshTokenService.save("old-jti", member, ttl);

        assertThat(refreshTokenService.rotate("old-jti", "new-jti", member, ttl)).isTrue();

        assertThat(refreshTokenService.isValid("old-jti", 1L)).isFalse();
        assertThat(refreshTokenService.isValid("new-jti", 1L)).isTrue();
        verify(memberTokenCacheService, never()).delete(1L);
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
