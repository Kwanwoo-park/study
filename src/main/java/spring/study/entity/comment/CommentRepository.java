package spring.study.entity.comment;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    static final String delete_comment = "delete from comment "
            + "where bid = :bid";

    public List<Comment> findByBid(Long bid, Sort sort);

    @Transactional
    @Modifying
    @Query(value = delete_comment, nativeQuery = true)
    public void deleteByBid(@Param("bid") Long bid);
}
