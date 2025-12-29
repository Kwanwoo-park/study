package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.chat.repository.ChatMessageRepository;

import java.util.Comparator;
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

    @Transactional
    public void saveAll(List<ChatMessageRequestDto> list) {
        chatMessageRepository.saveAll(list.stream().map(item -> ChatMessage.builder()
                .id(item.getId())
                .message(item.getMessage())
                .room(item.getRoom())
                .type(item.getType())
                .member(item.getMember())
                .build()).toList());
    }

    public ChatMessage findById(String id) {
        return chatMessageRepository.findById(id).orElseThrow();
    }

    public List<ChatMessage> find(ChatRoom room) {
        return chatMessageRepository.findByRoom(room).stream().sorted(Comparator.comparing(ChatMessage::getRegisterTime)).toList();
    }

    public void deleteByRoom(ChatRoom room) {
        chatMessageRepository.deleteByRoom(room);
    }

    public void deleteByMember(Member member) {
        chatMessageRepository.deleteByMember(member);
    }
}
