package spring.study.service.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.member.Member;
import spring.study.repository.chat.ChatMessageRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
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
