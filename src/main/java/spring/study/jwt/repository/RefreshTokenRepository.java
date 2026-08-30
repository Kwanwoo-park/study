package spring.study.jwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.study.jwt.entity.RefreshToken;

import java.time.Instant;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    boolean existsByJtiAndMemberIdAndExpiresAtAfter(String jti, Long memberId, Instant now);

    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, Instant now);

    List<RefreshToken> findByExpiresAtLessThanEqual(Instant now);

    List<RefreshToken> findTop50ByExpiresAtAfterOrderByExpiresAtDesc(Instant now);

    @Query("select distinct token.memberId from RefreshToken token")
    List<Long> findDistinctMemberIds();

    void deleteByMemberId(Long memberId);
}
