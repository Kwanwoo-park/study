package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatRoom;

import java.util.Collection;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    ChatRoom findByRoomId(String roomId);

    ChatRoom findByName(String name);

    List<ChatRoom> findByRoomIdIn(Collection<String> roomIds);

    @Transactional
    void deleteByRoomId(String roomId);
}
