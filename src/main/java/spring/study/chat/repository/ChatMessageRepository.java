package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByRoom(ChatRoom room);

    List<ChatMessage> findByRoom(ChatRoom room, Pageable pageable);

    long countByRoomAndMemberNotAndRegisterTimeAfter(ChatRoom room, Member member, LocalDateTime registerTime);

    List<ChatMessage> findByRegisterTimeBetween(LocalDateTime start, LocalDateTime end);

    @Transactional
    void deleteByRoom(ChatRoom room);

    @Transactional
    void deleteByMember(Member member);
}
