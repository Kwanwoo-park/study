package spring.study.jwt.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import spring.study.jwt.entity.RefreshToken;

import java.time.Instant;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Transactional(readOnly = true)
    boolean existsByJtiAndMemberIdAndExpiresAtAfter(String jti, Long memberId, Instant now);

    @Transactional(readOnly = true)
    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, Instant now);

    @Transactional(readOnly = true)
    List<RefreshToken> findByExpiresAtLessThanEqual(Instant now);

    @Transactional(readOnly = true)
    List<RefreshToken> findTop50ByExpiresAtAfterOrderByExpiresAtDesc(Instant now);

    @Query("select distinct token.memberId from RefreshToken token")
    @Transactional(readOnly = true)
    List<Long> findDistinctMemberIds();

    void deleteByMemberId(Long memberId);
}
