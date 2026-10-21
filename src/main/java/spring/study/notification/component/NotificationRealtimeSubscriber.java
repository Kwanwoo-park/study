package spring.study.notification.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import spring.study.common.service.EmitterService;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.dto.NotificationReadState;
import spring.study.notification.service.NotificationRealtimePublisher;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;

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
        IntegrationEventLogRoute route = NotificationRealtimePublisher.READ_CHANNEL.equals(
                new String(message.getChannel(), StandardCharsets.UTF_8))
                ? IntegrationEventLogRoute.REALTIME_NOTIFICATION_READ : IntegrationEventLogRoute.REALTIME_NOTIFICATION;
        try {
            if (route == IntegrationEventLogRoute.REALTIME_NOTIFICATION_READ) {
                NotificationReadState read = objectMapper.readValue(message.getBody(), NotificationReadState.class);
                notificationId = read.notificationId();
                emitterService.applyReadState(read);
                eventLogs.record(route, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, notificationId, 1, null);
                return;
            }
            NotificationRealtimeEvent event = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    NotificationRealtimeEvent.class
            );
            notificationId = event.id();
            emitterService.save(event.memberId().toString(), event);
            eventLogs.record(IntegrationEventLogRoute.REALTIME_NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, notificationId, 1, null);
        } catch (Exception exception) {
            eventLogs.record(route, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.FAILED, notificationId, 1, exception);
            log.error("Redis 실시간 알림 처리 실패", exception);
        }
    }
}
