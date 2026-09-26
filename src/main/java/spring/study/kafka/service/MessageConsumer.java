package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import spring.study.chat.service.ChatRealtimePublisher;
import spring.study.kafka.config.KafkaTopics;
import spring.study.notification.repository.NotificationRepository;
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
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final NotificationRealtimePublisher notificationRealtimePublisher;
    private final ChatMessageBatchService chatMessageBatchService;
    private final IntegrationEventLogService eventLogs;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "topic", containerFactory = "chatBatchKafkaListenerContainerFactory")
    public void consume(@Payload List<ChatMessageRequestDto> messages){
        try {
            List<ChatMessageRequestDto> savedMessages = chatMessageBatchService.saveBatch(messages);

            for (ChatMessageRequestDto message : savedMessages) {
                chatRealtimePublisher.publish(message);
            }
            eventLogs.record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, messages.size(), null);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.CHAT, Operation.CONSUME, Outcome.FAILED, null, messages == null ? 0 : messages.size(), exception);
            throw exception;
        }
    }

    @KafkaListener(topics = "topic2")
    @Transactional(readOnly = true)
    public void consumeNotification(ConsumerRecord<String, Notification> record) {
        Notification notification = record.value();
        try {
            if (notification == null || notification.getId() == null) throw new IllegalArgumentException("Invalid notification event");
            Notification current = notificationRepository.findById(notification.getId()).orElse(null);
            if (current == null) {
                eventLogs.record(Route.NOTIFICATION, Operation.CONSUME, Outcome.SKIPPED, notification.getId(), 1, null);
                return;
            }
            var idHeader = record.headers().lastHeader(KafkaTopics.EVENT_ID_HEADER);
            notificationRealtimePublisher.publish(current, idHeader == null ? null : new String(idHeader.value(), StandardCharsets.UTF_8));
            eventLogs.record(Route.NOTIFICATION, Operation.CONSUME, Outcome.SUCCESS, notification.getId(), 1, null);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.NOTIFICATION, Operation.CONSUME, Outcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw exception;
        }
    }
}
