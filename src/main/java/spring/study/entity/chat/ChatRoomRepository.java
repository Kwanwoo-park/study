package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    static final String delete_room = "delete from chatRoom "
            + "where roomId = :roomId";
    public ChatRoom findByRoomId(String roomId);

    @Transactional
    @Modifying
    @Query(value = delete_room, nativeQuery = true)
    public void deleteByRoomId(@Param("roomId") String roomId);
}
