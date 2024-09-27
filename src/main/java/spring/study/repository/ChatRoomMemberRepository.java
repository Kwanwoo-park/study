package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {
    public List<ChatRoomMember> findByRoom(ChatRoom room);

    public List<ChatRoomMember> findByMember(Member member);

    public ChatRoomMember findByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    public void deleteByMemberAndRoom(Member member, ChatRoom room);

    @Transactional
    public void deleteByMember(Member member);
}
