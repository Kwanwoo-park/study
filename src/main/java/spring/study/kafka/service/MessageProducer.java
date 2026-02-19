package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.notification.entity.Notification;

@Component
@RequiredArgsConstructor
public class MessageProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(ChatMessageRequestDto message){
        System.out.println(message.getEmail());
        kafkaTemplate.send("topic", message);
    }

    public void sendNotification(Notification notification) {
        System.out.println(notification.getMember().getName());
        kafkaTemplate.send("topic2", notification);
    }
}
