package spring.study.entity.comment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    public Comment findByBid(Long bid);
}
