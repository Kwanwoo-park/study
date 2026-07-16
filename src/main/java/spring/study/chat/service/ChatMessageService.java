package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatMessageResponseDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;
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

    public ChatMessage findById(String id) {
        return chatMessageRepository.findById(id).orElseThrow();
    }

    public List<ChatMessage> find(ChatRoom room) {
        return chatMessageRepository.findByRoom(room);
    }

    public List<ChatMessage> findActiveChatting(LocalDateTime start, LocalDateTime end) {
        return chatMessageRepository.findByRegisterTimeBetween(start, end);
    }

    public List<ChatMessageResponseDto> loadChatting(int cursor, int limit, ChatRoom room) {
        return chatMessageRepository.findByRoom(room, PageRequest.of(cursor, limit, Sort.by("registerTime").descending()))
                .stream()
                .map(ChatMessageResponseDto::new)
                .toList();
    }

    public long countUnread(ChatRoom room, Member member, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return chatMessageRepository.countByRoomAndMemberNot(room, member);
        }

        return chatMessageRepository.countByRoomAndMemberNotAndRegisterTimeAfter(room, member, lastReadAt);
    }

    public void deleteByRoom(ChatRoom room) {
        chatMessageRepository.deleteByRoom(room);
    }

    public void deleteByMember(Member member) {
        chatMessageRepository.deleteByMember(member);
    }

    public void deleteById(String id) {
        chatMessageRepository.deleteById(id);
    }
}
