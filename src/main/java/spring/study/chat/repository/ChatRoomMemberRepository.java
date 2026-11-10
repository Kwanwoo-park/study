package spring.study.chat.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    @Transactional(readOnly = true)
    List<ChatRoomMember> findByRoom(ChatRoom room);

    @EntityGraph(attributePaths = "member")
    @Transactional(readOnly = true)
    List<ChatRoomMember> findByRoomOrderByMember_NameAscMember_IdAsc(ChatRoom room);

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
