package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import spring.study.chat.component.WebSocketChatHandler;
import spring.study.chat.entity.ChatMessage;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.EmitterService;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final WebSocketChatHandler webSocketHandler;
    private final EmitterService emitterService;

    @KafkaListener(topics = "topic")
    public void consume(@Payload ChatMessage message){
        webSocketHandler.sendToEachSocket(message);
    }

    @KafkaListener(topics = "topic2")
    public void consume(@Payload Notification notification) {
        String id = notification.getMember().getId().toString();
        emitterService.save(id, notification);
    }
}
