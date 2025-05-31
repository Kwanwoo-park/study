package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    List<ChatRoomMember> findByRoom(ChatRoom room);

    ChatRoomMember findByRoomAndMemberNot(ChatRoom room, Member member);

    List<ChatRoomMember> findByMember(Member member);

    ChatRoomMember findByMemberAndRoom(Member member, ChatRoom room);

    boolean existsByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    void deleteByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    void deleteByMember(Member member);
}
