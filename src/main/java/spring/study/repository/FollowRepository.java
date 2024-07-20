package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.Follow;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    public Long countByFollower(Long follower);

    public List<Follow> findByFollower(Long follower);

    public Long countByFollowing(Long following);

    public List<Follow> findByFollowing(Long following);
}
