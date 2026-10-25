package spring.study.chat.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ChatMessageImgRepository extends JpaRepository<ChatMessageImg, Long> {
    @Transactional(readOnly = true)
    List<ChatMessageImg> findByMessageId(String messageId);

    @Query("""
            select new spring.study.chat.dto.ChatRoomImageResponse(i.id, m.id, i.imgSrc, m.registerTime, sender.name)
            from ChatMessageImg i
            join message m on m.id = i.messageId
            left join m.member sender
            left join ChatMessageHidden hidden on hidden.message = m and hidden.member = :viewer
            where m.room = :room
              and m.type = spring.study.chat.entity.MessageType.IMAGE
              and (m.status is null or m.status = spring.study.chat.entity.ChatMessageStatus.ACTIVE)
              and m.message <> :#{T(spring.study.chat.entity.ChatMessage).CENSORED_MESSAGE}
              and (:cursor is null or i.id < :cursor)
              and hidden.id is null
            order by i.id desc
            """)
    @Transactional(readOnly = true)
    List<ChatRoomImageResponse> findVisibleRoomImages(@Param("room") ChatRoom room, @Param("viewer") Member viewer, @Param("cursor") Long cursor, Pageable pageable);

    @Transactional
    void deleteByMessageId(String messageId);
}
