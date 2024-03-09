package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    static final String delete_roomMember = "delete from room_member where room_id = :roomId and mem_name = :name";

    public List<ChatMember> findByRoomId(String roomId);

    @Transactional
    @Modifying
    @Query(value = delete_roomMember, nativeQuery = true)
    public void deleteByRoomId(@Param("roomId") String roomId, @Param("name") String name);
}
