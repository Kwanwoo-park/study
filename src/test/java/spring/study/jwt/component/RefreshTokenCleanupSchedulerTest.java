package spring.study.jwt.component;

import org.junit.jupiter.api.Test;
import spring.study.jwt.facade.RefreshTokenFacade;
import spring.study.jwt.service.RefreshTokenService;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class RefreshTokenCleanupSchedulerTest {

    @Test
    void cleanupShouldDeleteExpiredTokensBeforeInactiveMemberTokens() {
        RefreshTokenFacade facade = mock(RefreshTokenFacade.class);
        RefreshTokenCleanupScheduler scheduler = new RefreshTokenCleanupScheduler(facade);

        scheduler.cleanup();

        var order = inOrder(facade);
        order.verify(facade).cleanUp();
    }
}
