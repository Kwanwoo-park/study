package spring.study.comment.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Collection;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Transactional(readOnly = true)
    List<Comment> findByBoard(Board board, Pageable pageable);

    @Transactional(readOnly = true)
    Boolean existsByMemberAndBoard(Member member, Board board);

    @Transactional(readOnly = true)
    long countByBoard(Board board);

    @Transactional
    void deleteByBoard(Board board);

    @Transactional
    void deleteByMember(Member member);

    @Query("select c.board.id, count(c.id) from comment c where c.board.id in :boardIds group by c.board.id")
    @Transactional(readOnly = true)
    List<Object[]> countByBoardIds(@Param("boardIds") Collection<Long> boardIds);
}
