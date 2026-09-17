package spring.study.chat.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    @Transactional(readOnly = true)
    List<ChatRoomMember> findByRoom(ChatRoom room);

    @Query("select rm from ChatRoomMember rm join fetch rm.member m where rm.room = :room order by m.name, m.id")
    @Transactional(readOnly = true)
    List<ChatRoomMember> findParticipantsByRoom(@Param("room") ChatRoom room);

    @Transactional(readOnly = true)
    List<ChatRoomMember> findByRoomAndMemberNot(ChatRoom room, Member member);

    @Transactional(readOnly = true)
    List<ChatRoomMember> findByMember(Member member);

    @Transactional(readOnly = true)
    ChatRoomMember findByMemberAndRoom(Member member, ChatRoom room);

    @Transactional(readOnly = true)
    boolean existsByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    void deleteByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    void deleteByMember(Member member);
}
