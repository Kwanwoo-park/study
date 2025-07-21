package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import spring.study.chat.component.WebSocketChatHandler;
import spring.study.chat.entity.ChatMessage;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final WebSocketChatHandler webSocketHandler;

    @KafkaListener(topics = "topic")
    public void consume(@Payload ChatMessage message){
        webSocketHandler.sendToEachSocket(message);
    }
}
