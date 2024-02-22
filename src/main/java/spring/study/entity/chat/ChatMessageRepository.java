package spring.study.entity.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    public ChatMessage findByRoomId(String roomId);
}
