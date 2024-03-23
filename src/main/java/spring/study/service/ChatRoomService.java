package spring.study.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatRoomService {
    private Map<String, ChatRoom> chatRooms;
    private final ChatRoomRepository chatRoomRepository;

    @PostConstruct
    private void init() { chatRooms = new LinkedHashMap<>(); }

    @Transactional
    public Long save(ChatRoom chatRoom) { return chatRoomRepository.save(chatRoom).getId(); }

    public ChatRoom findRoom(String roomId) { return chatRoomRepository.findByRoomId(roomId); }

    public List<ChatRoom> findAll() { return chatRoomRepository.findAll(); }

    public int updateRoomCountAdd(String roomId) { return chatRoomRepository.updateRoomCountAdd(roomId); }

    public int updateRoomCountSub(String roomId) { return chatRoomRepository.updateRoomCountSub(roomId); }

    public void deleteRoom(String roomId) { chatRoomRepository.deleteByRoomId(roomId); }

    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .count(1L)
                .build();

        save(chatRoom);

        chatRooms.put(randomId, chatRoom);

        return chatRoom;
    }
}
