package spring.study.repository.chat;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.member.Member;

import java.util.List;

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
