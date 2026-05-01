package spring.study.follow.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Transactional
    void deleteByFollowerAndFollowing(Member follower, Member following);

    @Transactional
    void deleteByFollower(Member follower);

    @Transactional
    void deleteByFollowing(Member following);

    Follow findByFollowerAndFollowing(Member follower, Member following);

    List<Follow> findByFollower(Member member);

    List<Follow> findByFollower(Member follower, Pageable pageable);
    List<Follow> findByFollowing(Member following, Pageable pageable);

    long countByFollower(Member follower);
    long countByFollowing(Member following);

    boolean existsByFollowerAndFollowing(Member follower, Member following);
}
