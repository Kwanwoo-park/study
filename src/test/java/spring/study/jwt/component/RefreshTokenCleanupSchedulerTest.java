package spring.study.jwt.component;

import org.junit.jupiter.api.Test;
import spring.study.jwt.service.RefreshTokenService;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class RefreshTokenCleanupSchedulerTest {

    @Test
    void cleanupShouldDeleteExpiredTokensBeforeInactiveMemberTokens() {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(refreshTokenService);

        scheduler.cleanup();

        var order = inOrder(refreshTokenService);
        order.verify(refreshTokenService).deleteExpiredTokens();
        order.verify(refreshTokenService).deleteInactiveMemberTokens();
    }
}
