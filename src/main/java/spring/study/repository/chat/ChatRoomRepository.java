package spring.study.repository.chat;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.chat.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    public ChatRoom findByRoomId(String roomId);

    public ChatRoom findByName(String name);

    @Transactional
    public void deleteByRoomId(String roomId);
}
