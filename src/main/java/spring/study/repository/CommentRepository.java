package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    public List<Comment> findByBoard(Board board);

    @Transactional
    public void deleteByBoard(Board board);

    @Transactional
    public void deleteByMember(Member member);
}
