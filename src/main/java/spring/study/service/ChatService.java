package spring.study.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.dto.chat.ChatMessageRequestDto;
import spring.study.dto.chat.ChatRoomRequestDto;
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatMessageRepository;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomRepository;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private Map<String, ChatRoom> chatRooms;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostConstruct
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    @Transactional
    public void save(ChatRoomRequestDto chatRoomRequestDto) { chatRoomRepository.save(chatRoomRequestDto.toEntity()); }

    @Transactional
    public void save(ChatMessageRequestDto chatMessageRequestDto) { chatMessageRepository.save(chatMessageRequestDto.toEntity()); }

    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll();
    }

    public List<ChatMessage> findAll(Sort sort) {
        return chatMessageRepository.findAll();
    }

    public ChatRoom findRoomId(String roomId) { return chatRoomRepository.findByRoomId(roomId); }

    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .build();

        chatRooms.put(randomId, chatRoom);
        return chatRoom;
    }
}
