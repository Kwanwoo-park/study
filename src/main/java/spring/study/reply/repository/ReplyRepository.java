package spring.study.reply.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.comment.entity.Comment;
import spring.study.reply.entity.Reply;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {
    @Transactional(readOnly = true)
    List<Reply> findByComment(Comment comment);
    @Transactional(readOnly = true)
    List<Reply> findByComment(Comment comment, Pageable pageable);
    @Transactional(readOnly = true)
    long countByComment(Comment comment);

    @Transactional
    void deleteByComment(Comment comment);

    @Transactional
    void deleteByMember(Member member);
}
