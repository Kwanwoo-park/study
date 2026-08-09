package spring.study.jwt.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.jwt.service.RefreshTokenService;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${security.jwt.refresh-token-cleanup-cron:0 0 * * * *}")
    public void cleanup() {
        refreshTokenService.deleteExpiredTokens();
        refreshTokenService.deleteInactiveMemberTokens();
    }
}
