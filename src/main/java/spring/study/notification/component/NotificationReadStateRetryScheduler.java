package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.notification.service.NotificationReadStateService;

@Component
@RequiredArgsConstructor
public class NotificationReadStateRetryScheduler {
    private final NotificationReadStateService readStateService;

    @Scheduled(fixedDelayString = "${notification.read-state.retry-delay-ms:30000}")
    public void retryReadStates() {
        readStateService.retryPending();
    }
}
