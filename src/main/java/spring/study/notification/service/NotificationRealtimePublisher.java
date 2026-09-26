package spring.study.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import spring.study.kafka.config.KafkaOperationsProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.entity.Notification;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
    public static final String CHANNEL = "notification-events";
    private static final DefaultRedisScript<Long> PUBLISH_ONCE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 1 then return -1 end
            local subscribers = redis.call('PUBLISH', ARGV[1], ARGV[2])
            if subscribers > 0 then redis.call('SET', KEYS[1], '1', 'EX', ARGV[3]) end
            return subscribers
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final IntegrationEventLogService eventLogs;
    private final KafkaOperationsProperties properties;

    public void publish(Notification notification) {
        publish(notification, null);
    }

    public void publish(Notification notification, String eventId) {
        try {
            String payload = objectMapper.writeValueAsString(NotificationRealtimeEvent.from(notification));
            String identity = eventId == null ? payload : eventId;
            String key = "notification:delivered:" + fingerprint(identity);
            Long subscribers = redisTemplate.execute(PUBLISH_ONCE, List.of(key), CHANNEL, payload, Long.toString(properties.getDedupTtlSeconds()));
            if (subscribers == null) throw new IllegalStateException("Redis delivery result unavailable");
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH,
                    subscribers == -1 ? Outcome.DUPLICATE : subscribers == 0 ? Outcome.NO_SUBSCRIBERS : Outcome.SUCCESS,
                    notification.getId(), 1, null, subscribers < 0 ? null : subscribers, null);
        } catch (JsonProcessingException exception) {
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw new IllegalStateException("실시간 알림을 직렬화할 수 없습니다", exception);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw exception;
        }
    }

    private static String fingerprint(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
