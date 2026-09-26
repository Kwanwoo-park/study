package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.config.KafkaOperationsProperties;
import java.time.Duration;

@Service @RequiredArgsConstructor
public class ChatRealtimePublisher {
    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messaging;
    private final KafkaOperationsProperties properties;

    public void publish(ChatMessageRequestDto message) {
        String key = "chat:realtime:delivered:" + message.getId();
        if (Boolean.TRUE.equals(redis.hasKey(key))) return;
        messaging.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
        // Only mark AFTER sending: a failed WebSocket send must remain retryable after the DB commit.
        // A crash between send and this marker can still redeliver; clients deduplicate by message ID.
        redis.opsForValue().set(key, "1", Duration.ofSeconds(properties.getDedupTtlSeconds()));
    }
}
