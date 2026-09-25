package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.service.ChatMessageBatchService;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.NotificationRealtimePublisher;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRealtimePublisher notificationRealtimePublisher;
    private final ChatMessageBatchService chatMessageBatchService;
    private final IntegrationEventLogService eventLogs;

    @KafkaListener(topics = "topic", containerFactory = "chatBatchKafkaListenerContainerFactory")
    public void consume(@Payload List<ChatMessageRequestDto> messages){
        try {
            List<ChatMessageRequestDto> savedMessages = chatMessageBatchService.saveBatch(messages);

            for (ChatMessageRequestDto message : savedMessages) {
                messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
            }
            eventLogs.record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, messages.size(), null);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.CHAT, Operation.CONSUME, Outcome.FAILED, null, messages == null ? 0 : messages.size(), exception);
            throw exception;
        }
    }

    @KafkaListener(topics = "topic2")
    public void consume(@Payload Notification notification) {
        try {
            notificationRealtimePublisher.publish(notification);
            eventLogs.record(Route.NOTIFICATION, Operation.CONSUME, Outcome.SUCCESS, notification.getId(), 1, null);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.NOTIFICATION, Operation.CONSUME, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw exception;
        }
    }
}
