package spring.study.member.jwt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    boolean existsByJtiAndMemberIdAndExpiresAtAfter(String jti, Long memberId, Instant now);

    void deleteByExpiresAtLessThanEqual(Instant now);
}
