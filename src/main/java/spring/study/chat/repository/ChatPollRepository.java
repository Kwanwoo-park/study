package spring.study.chat.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import spring.study.chat.entity.ChatPoll;
import spring.study.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatPollRepository extends JpaRepository<ChatPoll, Long> {
    List<ChatPoll> findByRoomOrderByIdDesc(ChatRoom room, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChatPoll> findForUpdateByIdAndRoom(Long id, ChatRoom room);
}
