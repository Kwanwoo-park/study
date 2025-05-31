package spring.study.comment.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBoard(Board board);

    @Transactional
    void deleteByBoard(Board board);

    @Transactional
    void deleteByMember(Member member);
}
