package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.entity.ChatRoom;
import spring.study.repository.ChatRoomRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatRoom save(ChatRoom chatRoom) {
        return chatRoomRepository.save(chatRoom);
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
