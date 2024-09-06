package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.ChatMessage;
import spring.study.entity.ChatRoom;
import spring.study.entity.Member;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    public ChatMessage findByRoom(ChatRoom room);

    @Transactional
    public void deleteByRoom(ChatRoom room);

    @Transactional
    public void deleteByMember(Member member);
}
