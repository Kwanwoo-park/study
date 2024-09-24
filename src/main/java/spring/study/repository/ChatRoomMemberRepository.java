package spring.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    public ChatRoomMember findByRoom(ChatRoom room);

    public ChatRoomMember findByMember(Member member);

    public ChatRoomMember findByMemberAndRoom(Member member, ChatRoom room);
}
