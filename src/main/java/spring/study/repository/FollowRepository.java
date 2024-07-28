package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.Follow;
import spring.study.entity.Member;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    @Transactional
    void deleteByFollower(Member follower);

    public Follow findByFollower(Member follower);
}
