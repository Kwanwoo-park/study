package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.Comment;

import java.util.List;

public interface TestRepository extends JpaRepository<Comment, Long> {
    static final String comment_join = "select c.id, c.comment, m.name, m.email, m.profile from comment c, member m where c.mid = m.id and c.bid = :bid";
    static final String message_join = "select m.name, s.message, m.profile from message s, member m where s.email = m.email and s.room_id = :roomId";

    @Transactional
    @Query(value = comment_join, nativeQuery = true)
    List<Object[]> getCommentWithMember(@Param("bid") Long bid);

    @Transactional
    @Query(value = message_join, nativeQuery = true)
    List<Object[]> getMessageWithMember(@Param("roomId") String roomId);
}
