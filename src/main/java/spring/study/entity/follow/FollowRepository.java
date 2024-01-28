package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    static final String unfollow = "delete from follow "
            + "where following = :following";

    @Transactional
    @Modifying
    @Query(value = unfollow, nativeQuery = true)
    public void unfollowing(@Param("following") Long following);
}
