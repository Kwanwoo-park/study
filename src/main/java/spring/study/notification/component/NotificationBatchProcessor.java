package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.common.repository.EmitterRepository;
import spring.study.notification.repository.NotificationRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationBatchProcessor {
    private final NotificationRepository notificationRepository;
    private final EmitterRepository emitterRepository;

    // every five minutes
    @Scheduled(cron = "0 0/5 * * * ?")
    public void deleteUnReadNotifications() {
        LocalDateTime cutOffTime = LocalDateTime.now().minusDays(1), beforeOneWeek = LocalDateTime.now().minusDays(7);

        List<Notification> notifications = notificationRepository.findByReadStatusAndRegisterTimeBefore(Status.READ, cutOffTime);

        notificationRepository.deleteByRegisterTimeBefore(beforeOneWeek);
        notificationRepository.deleteAll(notifications);
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartBeat() {
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitters();
        Map<String, String> data = new HashMap<>();

        data.put("message", null);

        sseEmitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().reconnectTime(3000).id(key).name("notification").data(data));
            } catch (Exception e) {
                emitterRepository.deleteById(key);
                emitter.complete();
            }
        });
    }
}
