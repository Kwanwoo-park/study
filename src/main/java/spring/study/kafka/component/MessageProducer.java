package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.entity.KafkaOutboxPayloadType;
import spring.study.kafka.dto.KafkaOutboxDispatchRequestedEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.entity.Notification;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;

@Component
@RequiredArgsConstructor
public class MessageProducer {
    private final KafkaOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IntegrationEventLogService eventLogs;

    public void sendMessage(ChatMessageRequestDto message){
        try {
            KafkaOutboxEvent event = KafkaOutboxEvent.builder()
                    .topic("topic")
                    .eventKey(message.getRoomId())
                    .payloadType(KafkaOutboxPayloadType.CHAT_MESSAGE)
                    .payload(objectMapper.writeValueAsString(message))
                    .build();

            outboxRepository.save(event);
            eventLogs.afterCommit(IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.QUEUE, IntegrationEventLogOutcome.QUEUED, event.getId(), 1, null, null);
            requestDispatch();
        } catch (JsonProcessingException exception) {
            eventLogs.record(IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.QUEUE, IntegrationEventLogOutcome.FAILED, null, 1, exception);
            throw new IllegalStateException("채팅 메시지를 발행 대기열에 저장할 수 없습니다", exception);
        } catch (RuntimeException exception) {
            eventLogs.record(IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.QUEUE, IntegrationEventLogOutcome.FAILED, null, 1, exception);
            throw exception;
        }
    }

    public void sendNotification(Notification notification) {
        try {
            KafkaOutboxEvent event = KafkaOutboxEvent.builder()
                    .topic("topic2")
                    .eventKey(notification.getMember().getId().toString())
                    .payloadType(KafkaOutboxPayloadType.NOTIFICATION)
                    .payload(notification.getId().toString())
                    .build();

            outboxRepository.save(event);
            eventLogs.afterCommit(IntegrationEventLogRoute.NOTIFICATION, IntegrationEventLogOperation.QUEUE, IntegrationEventLogOutcome.QUEUED, event.getId(), 1, null, null);
            requestDispatch();
        } catch (RuntimeException exception) {
            eventLogs.record(IntegrationEventLogRoute.NOTIFICATION, IntegrationEventLogOperation.QUEUE, IntegrationEventLogOutcome.FAILED, null, 1, exception);
            throw exception;
        }
    }

    private void requestDispatch() {
        eventPublisher.publishEvent(new KafkaOutboxDispatchRequestedEvent());
    }
}
