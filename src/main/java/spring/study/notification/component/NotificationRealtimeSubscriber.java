package spring.study.notification.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import spring.study.common.service.EmitterService;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimeSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final EmitterService emitterService;
    private final IntegrationEventLogService eventLogs;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Long notificationId = null;
        try {
            NotificationRealtimeEvent event = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    NotificationRealtimeEvent.class
            );
            notificationId = event.id();
            emitterService.save(event.memberId().toString(), event);
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.CONSUME, Outcome.SUCCESS, notificationId, 1, null);
        } catch (Exception exception) {
            eventLogs.record(Route.REALTIME_NOTIFICATION, Operation.CONSUME, Outcome.FAILED, notificationId, 1, exception);
            log.error("Redis 실시간 알림 처리 실패", exception);
        }
    }
}
