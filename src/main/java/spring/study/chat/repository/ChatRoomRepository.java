package spring.study.chat.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatRoom;

import java.util.Collection;
import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Transactional(readOnly = true)
    ChatRoom findByRoomId(String roomId);

    @Transactional(readOnly = true)
    ChatRoom findByName(String name);

    @Transactional(readOnly = true)
    List<ChatRoom> findByRoomIdIn(Collection<String> roomIds);

    @Transactional
    void deleteByRoomId(String roomId);
}
