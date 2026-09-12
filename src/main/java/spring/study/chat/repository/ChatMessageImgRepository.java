package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ChatMessageImgRepository extends JpaRepository<ChatMessageImg, Long> {
    List<ChatMessageImg> findByMessageId(String messageId);

    @Query("""
            select new spring.study.chat.dto.ChatRoomImageResponse(i.id, m.id, i.imgSrc, m.registerTime, sender.name)
            from ChatMessageImg i, message m left join m.member sender
            where i.messageId = m.id
              and m.room = :room
              and m.type = :imageType
              and (m.status is null or m.status = :activeStatus)
              and m.message <> :censoredMessage
              and (:cursor is null or i.id < :cursor)
              and not exists (
                  select h.id from ChatMessageHidden h
                  where h.message = m and h.member = :viewer
              )
            order by i.id desc
            """)
    List<ChatRoomImageResponse> findVisibleRoomImages(@Param("room") ChatRoom room,
            @Param("viewer") Member viewer, @Param("cursor") Long cursor,
            @Param("imageType") MessageType imageType, @Param("activeStatus") ChatMessageStatus activeStatus,
            @Param("censoredMessage") String censoredMessage, Pageable pageable);

    @Transactional
    void deleteByMessageId(String messageId);
}
