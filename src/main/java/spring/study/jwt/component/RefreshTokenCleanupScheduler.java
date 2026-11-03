package spring.study.jwt.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.jwt.facade.RefreshTokenFacade;

@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenFacade facade;

    @Scheduled(cron = "${security.jwt.refresh-token-cleanup-cron:0 0 * * * *}")
    public void cleanup() {
        facade.cleanUp();
    }
}
