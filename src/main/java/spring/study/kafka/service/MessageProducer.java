package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.entity.Notification;

@Component
@RequiredArgsConstructor
public class MessageProducer {
    private final KafkaOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void sendMessage(ChatMessageRequestDto message){
        try {
            outboxRepository.save(new KafkaOutboxEvent(
                    "topic",
                    message.getRoomId(),
                    KafkaOutboxEvent.PayloadType.CHAT_MESSAGE,
                    objectMapper.writeValueAsString(message)
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("채팅 메시지를 발행 대기열에 저장할 수 없습니다", exception);
        }
    }

    public void sendNotification(Notification notification) {
        outboxRepository.save(new KafkaOutboxEvent(
                "topic2",
                notification.getMember().getId().toString(),
                KafkaOutboxEvent.PayloadType.NOTIFICATION,
                notification.getId().toString()
        ));
    }
}
