package spring.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatMessageRepository;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomRepository;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final ObjectMapper objectMapper;
    private Map<String, ChatRoom> chatRooms;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @PostConstruct
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    @Transactional
    public void save(ChatRoom chatRoom) { chatRoomRepository.save(chatRoom); }

    @Transactional
    public void save(ChatMessage chatMessage) { chatMessageRepository.save(chatMessage); }

    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll();
    }

    public ChatRoom findRoom(String roomId) { return chatRoomRepository.findByRoomId(roomId); }

    public ChatMessage findMessage(String roomId) { return chatMessageRepository.findByRoomId(roomId); }

    public void deleteRoom(String roomId) { chatRoomRepository.deleteByRoomId(roomId); }

    public void deleteMessageBySender(String roomId, String name) { chatMessageRepository.deleteMessage(roomId, name);}

    public void deleteMessage(String roomId) { chatMessageRepository.deleteMessage(roomId);}

    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .build();

        save(chatRoom);

        chatRooms.put(randomId, chatRoom);
        return chatRoom;
    }
}
