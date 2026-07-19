package spring.study.jwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.jwt.entity.RefreshToken;

import java.time.Instant;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    boolean existsByJtiAndMemberIdAndExpiresAtAfter(String jti, Long memberId, Instant now);

    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, Instant now);

    List<RefreshToken> findByExpiresAtLessThanEqual(Instant now);
}
