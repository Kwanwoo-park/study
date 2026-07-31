package spring.study.chat.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByRoom(ChatRoom room);

    List<ChatMessage> findByRoom(ChatRoom room, Pageable pageable);

    @Query("""
            select m
            from message m
            where m.room = :room
              and not exists (
                  select h.id
                  from ChatMessageHidden h
                  where h.message = m and h.member = :member
              )
            """)
    List<ChatMessage> findVisibleByRoom(@Param("room") ChatRoom room,
                                        @Param("member") Member member,
                                        Pageable pageable);

    @Query("""
            select count(m)
            from message m
            where m.room = :room
              and m.member <> :member
              and (m.status is null or m.status = :activeStatus)
              and not exists (
                  select h.id
                  from ChatMessageHidden h
                  where h.message = m and h.member = :member
              )
            """)
    long countVisibleUnread(@Param("room") ChatRoom room,
                            @Param("member") Member member,
                            @Param("activeStatus") ChatMessageStatus activeStatus);

    @Query("""
            select count(m)
            from message m
            where m.room = :room
              and m.member <> :member
              and m.registerTime > :registerTime
              and (m.status is null or m.status = :activeStatus)
              and not exists (
                  select h.id
                  from ChatMessageHidden h
                  where h.message = m and h.member = :member
              )
            """)
    long countVisibleUnreadAfter(@Param("room") ChatRoom room,
                                 @Param("member") Member member,
                                 @Param("registerTime") LocalDateTime registerTime,
                                 @Param("activeStatus") ChatMessageStatus activeStatus);

    Optional<ChatMessage> findFirstByRoomAndStatusOrderByRegisterTimeDesc(ChatRoom room,
                                                                         ChatMessageStatus status);

    List<ChatMessage> findByRegisterTimeBetween(LocalDateTime start, LocalDateTime end);

    @Transactional
    void deleteByRoom(ChatRoom room);

    @Transactional
    void deleteByMember(Member member);
}
