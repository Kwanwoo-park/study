package spring.study.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.member.entity.Member;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.repository.RefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberTokenCacheService memberTokenCacheService;

    public void save(String jti, Member member, Duration ttl) {
        refreshTokenRepository.save(new RefreshToken(jti, member.getId(), ttl));
        memberTokenCacheService.save(member, ttl);
    }

    public boolean rotate(String oldJti, String newJti, Member member, Duration ttl) {
        RefreshToken oldToken = refreshTokenRepository.findById(oldJti).orElse(null);
        Instant now = Instant.now();
        if (oldToken == null
                || !oldToken.getMemberId().equals(member.getId())
                || !oldToken.getExpiresAt().isAfter(now)) {
            return false;
        }

        refreshTokenRepository.delete(oldToken);
        refreshTokenRepository.save(new RefreshToken(newJti, member.getId(), ttl));
        memberTokenCacheService.save(member, ttl);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isValid(String jti, Long memberId) {
        return refreshTokenRepository.existsByJtiAndMemberIdAndExpiresAtAfter(jti, memberId, Instant.now());
    }

    public void revoke(String jti) {
        refreshTokenRepository.findById(jti).ifPresent(refreshToken -> {
            refreshTokenRepository.delete(refreshToken);
            refreshTokenRepository.flush();
            deleteMemberCacheIfNoValidToken(refreshToken.getMemberId(), Instant.now());
        });
    }

    @Scheduled(cron = "${security.jwt.refresh-token-cleanup-cron:0 0 * * * *}")
    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        List<RefreshToken> expiredTokens = refreshTokenRepository.findByExpiresAtLessThanEqual(now);
        refreshTokenRepository.deleteAllInBatch(expiredTokens);
        expiredTokens.stream()
                .map(RefreshToken::getMemberId)
                .distinct()
                .forEach(memberId -> deleteMemberCacheIfNoValidToken(memberId, now));
    }

    private void deleteMemberCacheIfNoValidToken(Long memberId, Instant now) {
        if (!refreshTokenRepository.existsByMemberIdAndExpiresAtAfter(memberId, now)) {
            memberTokenCacheService.delete(memberId);
        }
    }
}
