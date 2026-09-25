package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.event.KafkaOutboxDispatchRequestedEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.entity.Notification;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

@Component
@RequiredArgsConstructor
public class MessageProducer {
    private final KafkaOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IntegrationEventLogService eventLogs;

    public void sendMessage(ChatMessageRequestDto message){
        try {
            KafkaOutboxEvent event = new KafkaOutboxEvent(
                    "topic",
                    message.getRoomId(),
                    KafkaOutboxEvent.PayloadType.CHAT_MESSAGE,
                    objectMapper.writeValueAsString(message)
            );
            outboxRepository.save(event);
            eventLogs.afterCommit(Route.CHAT, Operation.QUEUE, Outcome.QUEUED, event.getId(), 1, null, null);
            requestDispatch();
        } catch (JsonProcessingException exception) {
            eventLogs.record(Route.CHAT, Operation.QUEUE, Outcome.FAILED, null, 1, exception);
            throw new IllegalStateException("채팅 메시지를 발행 대기열에 저장할 수 없습니다", exception);
        } catch (RuntimeException exception) {
            eventLogs.record(Route.CHAT, Operation.QUEUE, Outcome.FAILED, null, 1, exception);
            throw exception;
        }
    }

    public void sendNotification(Notification notification) {
        try {
            KafkaOutboxEvent event = new KafkaOutboxEvent(
                    "topic2",
                    notification.getMember().getId().toString(),
                    KafkaOutboxEvent.PayloadType.NOTIFICATION,
                    notification.getId().toString()
            );
            outboxRepository.save(event);
            eventLogs.afterCommit(Route.NOTIFICATION, Operation.QUEUE, Outcome.QUEUED, event.getId(), 1, null, null);
            requestDispatch();
        } catch (RuntimeException exception) {
            eventLogs.record(Route.NOTIFICATION, Operation.QUEUE, Outcome.FAILED, null, 1, exception);
            throw exception;
        }
    }

    private void requestDispatch() {
        eventPublisher.publishEvent(new KafkaOutboxDispatchRequestedEvent());
    }
}
