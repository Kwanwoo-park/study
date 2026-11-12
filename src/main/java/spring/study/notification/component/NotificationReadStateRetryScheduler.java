package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReadStateRetryScheduler {
    private final NotificationReadStateCoordinator readStateCoordinator;

    @Scheduled(fixedDelayString = "${notification.read-state.retry-delay-ms:30000}")
    public void retryReadStates() {
        readStateCoordinator.retryPending();
    }
}
