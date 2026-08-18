package spring.study.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.entity.Notification;

@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
    public static final String CHANNEL = "notification-events";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Notification notification) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(NotificationRealtimeEvent.from(notification)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("실시간 알림을 직렬화할 수 없습니다", exception);
        }
    }
}
