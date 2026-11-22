package spring.study.jwt.repository;

import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.dto.RefreshTokenMemberIdProjection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Transactional(readOnly = true)
    boolean existsByJtiAndMemberIdAndExpiresAtAfter(String jti, Long memberId, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findForUpdateByJtiAndMemberId(String jti, Long memberId);

    @Transactional(readOnly = true)
    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, Instant now);

    @Transactional(readOnly = true)
    List<RefreshToken> findByExpiresAtLessThanEqual(Instant now);

    @Transactional(readOnly = true)
    List<RefreshToken> findTop50ByExpiresAtAfterOrderByExpiresAtDesc(Instant now);

    @Transactional(readOnly = true)
    List<RefreshTokenMemberIdProjection> findDistinctBy();

    void deleteByMemberId(Long memberId);
}
