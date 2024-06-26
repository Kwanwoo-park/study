package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    static String delete_message = "delete from message where room_id = :roomId";

    public List<ChatMessage> findByRoomId(String roomId);

    @Transactional
    @Modifying
    @Query(value = delete_message, nativeQuery = true)
    public void deleteMessage(@Param("roomId") String roomId);
}
