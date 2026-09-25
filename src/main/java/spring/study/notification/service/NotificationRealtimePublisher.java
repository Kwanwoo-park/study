package spring.study.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.entity.Notification;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
    public static final String CHANNEL = "notification-events";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final IntegrationEventLogService eventLogs;

    public void publish(Notification notification) {
        try {
            Long subscribers = redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(NotificationRealtimeEvent.from(notification)));
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH,
                    Long.valueOf(0).equals(subscribers) ? Outcome.NO_SUBSCRIBERS : Outcome.SUCCESS,
                    notification.getId(), 1, null, subscribers, null);
        } catch (JsonProcessingException exception) {
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw new IllegalStateException("실시간 알림을 직렬화할 수 없습니다", exception);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw exception;
        }
    }
}
