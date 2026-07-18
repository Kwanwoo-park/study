package spring.study.member.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Transactional
public class RefreshTokenStore {
    private final RefreshTokenRepository refreshTokenRepository;

    public void save(String jti, Long memberId, Duration ttl) {
        refreshTokenRepository.save(new RefreshToken(jti, memberId, ttl));
    }

    @Transactional(readOnly = true)
    public boolean isValid(String jti, Long memberId) {
        return refreshTokenRepository.existsByJtiAndMemberIdAndExpiresAtAfter(jti, memberId, Instant.now());
    }

    public void revoke(String jti) {
        refreshTokenRepository.deleteById(jti);
    }

    @Scheduled(cron = "${security.jwt.refresh-token-cleanup-cron:0 0 * * * *}")
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtLessThanEqual(Instant.now());
    }
}
