package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationBatchProcessor {
    private final NotificationRepository notificationRepository;

    // every five minutes
    @Scheduled(cron = "0 0/5 * * * ?")
    public void deleteUnReadNotifications() {
        LocalDateTime cutOffTime = LocalDateTime.now().minusDays(1);
        List<Notification> notifications = notificationRepository.findByReadStatusAndRegisterTimeBefore(Status.READ, cutOffTime);

        notificationRepository.deleteAll(notifications);
    }
}
