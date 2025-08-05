package spring.study.chat.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import spring.study.chat.entity.ChatMessage;

@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String msg = new String(message.getBody());
            ChatMessage chatMessage = objectMapper.readValue(msg, ChatMessage.class);

            messagingTemplate.convertAndSend(
                    "/topic/chat/room" + chatMessage.getRoom().getRoomId(),
                    chatMessage
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
