package spring.study.chat.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageHidden;
import spring.study.member.entity.Member;

public interface ChatMessageHiddenRepository extends JpaRepository<ChatMessageHidden, Long> {
    @Transactional(readOnly = true)
    boolean existsByMessageAndMember(ChatMessage message, Member member);
}
