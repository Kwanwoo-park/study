package spring.study.jwt.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.repository.RefreshTokenRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

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
    private MemberRepository memberRepository;

    @MockBean
    private MemberTokenCacheService memberTokenCacheService;

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

    @Test
    void cleanupDeletesAllTokensAndCacheOnlyForMembersInactiveForFifteenDays() {
        Member inactiveMember = persistedMember(
                "inactive@example.com", "01011112222", LocalDateTime.now().minusDays(16));
        Member activeMember = persistedMember(
                "active@example.com", "01033334444", LocalDateTime.now().minusDays(1));
        refreshTokenRepository.save(new RefreshToken(
                "inactive-first-jti", inactiveMember.getId(), Duration.ofDays(30)));
        refreshTokenRepository.save(new RefreshToken(
                "inactive-second-jti", inactiveMember.getId(), Duration.ofDays(30)));
        refreshTokenRepository.save(new RefreshToken(
                "active-jti", activeMember.getId(), Duration.ofDays(30)));
        clearInvocations(memberTokenCacheService);

        refreshTokenService.deleteInactiveMemberTokens();

        assertThat(refreshTokenRepository.existsById("inactive-first-jti")).isFalse();
        assertThat(refreshTokenRepository.existsById("inactive-second-jti")).isFalse();
        assertThat(refreshTokenRepository.existsById("active-jti")).isTrue();
        verify(memberTokenCacheService).delete(inactiveMember.getId());
        verify(memberTokenCacheService, never()).delete(activeMember.getId());
    }

    private Member persistedMember(String email, String phone, LocalDateTime lastLoginTime) {
        return memberRepository.saveAndFlush(Member.builder()
                .email(email)
                .pwd("encoded-password")
                .name("user")
                .role(Role.USER)
                .phone(phone)
                .birth("20000101")
                .profile("profile.png")
                .lastLoginTime(lastLoginTime)
                .build());
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
