package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.Follow;
import spring.study.entity.Member;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Transactional
    void deleteByFollowerAndFollowing(Member follower, Member following);

    @Transactional
    void deleteByFollower(Member follower);

    @Transactional
    void deleteByFollowing(Member following);

    public Follow findByFollowerAndFollowing(Member follower, Member following);

    public boolean existsByFollowerAndFollowing(Member follower, Member following);
}
