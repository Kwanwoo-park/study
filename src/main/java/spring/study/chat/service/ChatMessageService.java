package spring.study.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.chat.repository.ChatMessageRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final MessageProducer producer;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public void save(ChatMessage message) {
        if (message.getType().equals(MessageType.ENTER))
            message.setMessage(message.getMember().getName() + "님이 입장했습니다.");
        else if (message.getType().equals(MessageType.QUIT))
            message.setMessage(message.getMember().getName() + "님이 퇴장했습니다.");

        producer.sendMessage(message);

        try {
            String key = "chat:message:roomId:"+message.getRoom().getRoomId();
            String json = objectMapper.writeValueAsString(message);

            objectRedisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public List<ChatMessage> saveAll(List<ChatMessage> list) {
        return chatMessageRepository.saveAll(list);
    }

    public List<ChatMessage> find(ChatRoom room) {
        return chatMessageRepository.findByRoom(room);
    }

    public void deleteByRoom(ChatRoom room) {
        chatMessageRepository.deleteByRoom(room);
    }

    public void deleteByMember(Member member) {
        chatMessageRepository.deleteByMember(member);
    }
}
