package spring.study.follow.repository;

import org.springframework.transaction.annotation.Transactional;
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

    @Transactional(readOnly = true)
    Follow findByFollowerAndFollowing(Member follower, Member following);

    @Transactional(readOnly = true)
    List<Follow> findByFollower(Member member);

    @Transactional(readOnly = true)
    List<Follow> findByFollower(Member follower, Pageable pageable);
    @Transactional(readOnly = true)
    List<Follow> findByFollowing(Member following, Pageable pageable);

    @Transactional(readOnly = true)
    long countByFollower(Member follower);
    @Transactional(readOnly = true)
    long countByFollowing(Member following);

    @Transactional(readOnly = true)
    boolean existsByFollowerAndFollowing(Member follower, Member following);

    @Query("""
            select f from follow f
            where f.following = :target
              and (f.follower.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.follower = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.follower))
            """)
    @Transactional(readOnly = true)
    List<Follow> findVisibleFollowers(@Param("target") Member target, @Param("viewer") Member viewer, Pageable pageable);

    @Query("""
            select count(f) from follow f
            where f.following = :target
              and (f.follower.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.follower = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.follower))
            """)
    @Transactional(readOnly = true)
    long countVisibleFollowers(@Param("target") Member target, @Param("viewer") Member viewer);

    @Query("""
            select f from follow f
            where f.follower = :target
              and (f.following.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.following = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.following))
            """)
    @Transactional(readOnly = true)
    List<Follow> findVisibleFollowing(@Param("target") Member target, @Param("viewer") Member viewer, Pageable pageable);

    @Query("""
            select count(f) from follow f
            where f.follower = :target
              and (f.following.visibility = spring.study.common.entity.CommonVisibility.PUBLIC
                   or f.following = :viewer
                   or exists (select visibleFollow.id from follow visibleFollow
                              where visibleFollow.follower = :viewer and visibleFollow.following = f.following))
            """)
    @Transactional(readOnly = true)
    long countVisibleFollowing(@Param("target") Member target, @Param("viewer") Member viewer);
}
