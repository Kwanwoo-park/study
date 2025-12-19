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

    @Transactional
    public List<ChatMessage> saveAll(List<ChatMessage> list) {
        return chatMessageRepository.saveAll(list);
    }

    public ChatMessage findById(Long id) {
        return chatMessageRepository.findById(id).orElseThrow();
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
