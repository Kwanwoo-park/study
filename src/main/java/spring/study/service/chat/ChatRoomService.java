package spring.study.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.dto.chat.ChatRoomResponseDto;
import spring.study.entity.chat.ChatRoom;
import spring.study.repository.chat.ChatRoomRepository;

import java.util.*;
import java.util.stream.Collectors;

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

    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<ChatRoom> list = chatRoomRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(ChatRoomResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
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
