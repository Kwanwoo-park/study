package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.config.KafkaOperationsProperties;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ChatRealtimePublisherTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final SimpMessagingTemplate socket = mock(SimpMessagingTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ChatRealtimePublisher publisher = new ChatRealtimePublisher(redis, socket, new KafkaOperationsProperties());
    private final ChatMessageRequestDto message = ChatMessageRequestDto.builder().id("m1").roomId("r1").build();

    @Test
    void failedSendRemainsRetryableAndSuccessfulRetryIsRemembered() {
        doThrow(new IllegalStateException("disconnected")).doNothing().when(socket).convertAndSend("/sub/chat/room/r1", message);
        assertThatThrownBy(() -> publisher.publish(message)).isInstanceOf(IllegalStateException.class);
        verify(redis, never()).opsForValue();
        when(redis.opsForValue()).thenReturn(values);
        publisher.publish(message);
        var order = inOrder(socket, values);
        order.verify(socket, times(2)).convertAndSend("/sub/chat/room/r1", message);
        order.verify(values).set(eq("chat:realtime:delivered:m1"), eq("1"), any(java.time.Duration.class));
        when(redis.hasKey("chat:realtime:delivered:m1")).thenReturn(true);
        publisher.publish(message);
        verify(socket, times(2)).convertAndSend("/sub/chat/room/r1", message);
    }
}
