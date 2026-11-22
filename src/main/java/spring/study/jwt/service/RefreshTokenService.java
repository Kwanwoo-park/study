package spring.study.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.member.entity.Member;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.repository.RefreshTokenRepository;
import spring.study.jwt.dto.RefreshTokenMemberIdProjection;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void save(String jti, Member member, Duration ttl) {
        save(jti, member, ttl, null);
    }

    @Transactional
    public void save(String jti, Member member, Duration ttl, String ipAddress) {
        refreshTokenRepository.save(new RefreshToken(jti, member.getId(), ttl, ipAddress));
    }

    @Transactional(readOnly = true)
    public RefreshToken findOldToken(String oldJti) {
        return refreshTokenRepository.findById(oldJti).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Long> findDistinctMemberIds() {
        return refreshTokenRepository.findDistinctBy().stream().map(RefreshTokenMemberIdProjection::memberId).toList();
    }

    @Transactional
    public void deleteOldToken(RefreshToken oldToken) {
        refreshTokenRepository.delete(oldToken);
        refreshTokenRepository.flush();
    }

    @Transactional(readOnly = true)
    public boolean isValid(String jti, Long memberId) {
        return refreshTokenRepository.existsByJtiAndMemberIdAndExpiresAtAfter(jti, memberId, Instant.now());
    }

    public boolean replace(String oldJti, String newJti, Member member, Duration ttl, String ipAddress) {
        RefreshToken current = refreshTokenRepository.findForUpdateByJtiAndMemberId(oldJti, member.getId()).orElse(null);
        if (current == null || !current.getExpiresAt().isAfter(Instant.now())) return false;

        refreshTokenRepository.delete(current);
        refreshTokenRepository.saveAndFlush(new RefreshToken(newJti, member.getId(), ttl, ipAddress));
        return true;
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> findByExpiresAtLessThanEqual(Instant now) {
        return refreshTokenRepository.findByExpiresAtLessThanEqual(now);
    }

    @Transactional
    public void revoke(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
        refreshTokenRepository.flush();
    }

    @Transactional
    public void deleteAllInBatch(List<RefreshToken> expiredTokens) {
        refreshTokenRepository.deleteAllInBatch(expiredTokens);
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> findAllMemberIds() {
        return refreshTokenRepository.findTop50ByExpiresAtAfterOrderByExpiresAtDesc(Instant.now());
    }

    @Transactional(readOnly = true)
    public boolean existsByMemberIdAndExpiresAtAfter(Long memberId, Instant now) {
        return refreshTokenRepository.existsByMemberIdAndExpiresAtAfter(memberId, now);
    }
}
