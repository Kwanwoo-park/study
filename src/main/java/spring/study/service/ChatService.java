package spring.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.*;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final ObjectMapper objectMapper;
    private Map<String, ChatRoom> chatRooms;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;

    @PostConstruct
    private void init() {
        chatRooms = new LinkedHashMap<>();
    }

    @Transactional
    public Long save(ChatRoom chatRoom) { return chatRoomRepository.save(chatRoom).getId(); }

    @Transactional
    public Long save(ChatMessage chatMessage) { return chatMessageRepository.save(chatMessage).getId(); }

    @Transactional
    public Long save(ChatMember chatMember) { return chatMemberRepository.save(chatMember).getId(); }

    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll();
    }

    public ChatRoom findRoom(String roomId) { return chatRoomRepository.findByRoomId(roomId); }

    public ChatMessage findMessage(String roomId) { return chatMessageRepository.findByRoomId(roomId); }

    public ChatMember findMember(String roomId) { return chatMemberRepository.findByRoomId(roomId); }

    public void deleteRoom(String roomId) { chatRoomRepository.deleteByRoomId(roomId); }

    public void deleteMessage(String roomId) { chatMessageRepository.deleteMessage(roomId);}

    public void deleteRoomMember(String roomId, String name) { chatMemberRepository.deleteByRoomId(roomId, name);}

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
