package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;

import java.util.List;

@Repository
public interface ChatMessageImgRepository extends JpaRepository<ChatMessageImg, Long> {
    List<ChatMessageImg> findByMessageId(String messageId);

    @Transactional
    void deleteByMessage(ChatMessage message);
}
