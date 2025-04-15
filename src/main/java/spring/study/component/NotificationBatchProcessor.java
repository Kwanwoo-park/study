package spring.study.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.entity.notification.Notification;
import spring.study.repository.notification.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationBatchProcessor {
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void deleteUnReadNotifications() {
        LocalDateTime cutOffTime = LocalDateTime.now().minusMinutes(10);
        List<Notification> notifications = notificationRepository.findByIsReadAndRegisterTimeBefore(false, cutOffTime);

        notificationRepository.deleteAll(notifications);
    }
}
