package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    static final String unfollow = "delete from follow "
            + "where follower = :follower and following = :following";

    @Transactional
    @Modifying
    @Query(value = unfollow, nativeQuery = true)
    public int unfollowing(@Param("follower") Long follower, @Param("following") Long following);

    public Long countByFollower(Long follower);

    public List<Follow> findByFollower(Long follower);

    public Long countByFollowing(Long following);

    public List<Follow> findByFollowing(Long following);
}
