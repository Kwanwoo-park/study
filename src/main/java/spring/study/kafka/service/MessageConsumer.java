package spring.study.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import spring.study.chat.entity.ChatMessage;
import spring.study.notification.entity.Notification;
import spring.study.common.service.EmitterService;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {
    private final SimpMessagingTemplate messagingTemplate;
    private final EmitterService emitterService;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @KafkaListener(topics = "topic")
    public void consume(@Payload ChatMessage message){
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoom().getRoomId(), message);

        try {
            String key = "chat:message:roomId:"+message.getRoom().getRoomId();
            String json = objectMapper.writeValueAsString(message);

            objectRedisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @KafkaListener(topics = "topic2")
    public void consume(@Payload Notification notification) {
        String id = notification.getMember().getId().toString();
        emitterService.save(id, notification);
    }
}
