package spring.study.follow.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select f from follow f
            where f.following = :target
              and (f.follower.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.follower = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.follower))
            """)
    List<Follow> findVisibleFollowers(@Param("target") Member target, @Param("viewer") Member viewer, Pageable pageable);

    @Query("""
            select count(f) from follow f
            where f.following = :target
              and (f.follower.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.follower = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.follower))
            """)
    long countVisibleFollowers(@Param("target") Member target, @Param("viewer") Member viewer);

    @Query("""
            select f from follow f
            where f.follower = :target
              and (f.following.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.following = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.following))
            """)
    List<Follow> findVisibleFollowing(@Param("target") Member target, @Param("viewer") Member viewer, Pageable pageable);

    @Query("""
            select count(f) from follow f
            where f.follower = :target
              and (f.following.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.following = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.following))
            """)
    long countVisibleFollowing(@Param("target") Member target, @Param("viewer") Member viewer);
}
