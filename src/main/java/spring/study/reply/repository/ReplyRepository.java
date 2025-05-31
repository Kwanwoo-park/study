package spring.study.reply.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.comment.entity.Comment;
import spring.study.reply.entity.Reply;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByComment(Comment comment);

    @Transactional
    void deleteByComment(Comment comment);

    @Transactional
    void deleteByMember(Member member);
}
