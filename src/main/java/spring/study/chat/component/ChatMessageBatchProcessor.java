package spring.study.chat.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.service.ChatMessageService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageBatchProcessor {
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 5000)
    public void flushMessagesToDB() {
        Cursor<byte[]> cursor = objectRedisTemplate.getConnectionFactory().getConnection()
                .scan(ScanOptions.scanOptions().match("chat:message:roomId:*").build());

        while (cursor.hasNext()) {
            String key = new String(cursor.next());

            ListOperations<String, Object> ops = objectRedisTemplate.opsForList();

            Long size = ops.size(key);

            if (size != null && size > 0) {
                List<Object> messages = ops.range(key, 0, -1);

                if (messages != null) {
                    List<ChatMessage> entities = messages.stream()
                            .map(obj -> {
                                try {
                                    return objectMapper.readValue(obj.toString(), ChatMessage.class);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .toList();

                    chatMessageService.saveAll(entities);
                    objectRedisTemplate.delete(key);
                }
            }
        }
    }
}
