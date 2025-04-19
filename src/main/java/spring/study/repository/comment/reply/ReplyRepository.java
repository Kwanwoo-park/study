package spring.study.repository.comment.reply;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;

import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByComment(Comment comment);

    @Transactional
    void deleteByComment(Comment comment);

    @Transactional
    void deleteByMember(Member member);
}
