package spring.study.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.entity.ChatRoom;
import spring.study.repository.ChatRoomRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {
    private final ObjectMapper objectMapper;
    private Map<String, ChatRoom> chatRoomMap;
    private final ChatRoomRepository chatRoomRepository;

    @PostConstruct
    void init() {
        chatRoomMap = new LinkedHashMap<>();
    }

    @Transactional
    public ChatRoom createRoom(String name) {
        String randomId = UUID.randomUUID().toString();

        ChatRoom room = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .count(1L)
                .build();

        ChatRoom save = chatRoomRepository.save(room);

        chatRoomMap.put(randomId, save);

        return save;
    }

    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll();
    }

    public ChatRoom find(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    public void delete(String roomId) {
        chatRoomRepository.deleteByRoomId(roomId);
    }

    @Transactional
    public void addCount(Long id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 채팅방입니다."
        ));

        chatRoom.addCount();
    }

    @Transactional
    public void subCount(Long id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 채팅방입니다"
        ));

        chatRoom.subCount();
    }
}
