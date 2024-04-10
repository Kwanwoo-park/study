package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    static final String delete_room = "delete from room "
            + "where room_id = :roomId";

    static final String add_count = "update room "
            + "set count = count + 1 "
            + "where room_id = :roomId";

    static final String sub_count = "update room "
            + "set count = count -1 "
            + "where room_id = :roomId";

    public ChatRoom findByRoomId(String roomId);

    @Transactional
    @Modifying
    @Query(value = delete_room, nativeQuery = true)
    public void deleteByRoomId(@Param("roomId") String roomId);

    @Transactional
    @Modifying
    @Query(value = add_count, nativeQuery = true)
    public int updateRoomCountAdd(@Param("roomId") String roomId);

    @Transactional
    @Modifying
    @Query(value = sub_count, nativeQuery = true)
    public int updateRoomCountSub(@Param("roomId") String roomId);
}
