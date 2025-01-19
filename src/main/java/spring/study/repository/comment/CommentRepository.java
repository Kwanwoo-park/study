package spring.study.repository.comment;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.board.Board;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    public List<Comment> findByBoard(Board board);

    @Transactional
    public void deleteByBoard(Board board);

    @Transactional
    public void deleteByMember(Member member);
}
