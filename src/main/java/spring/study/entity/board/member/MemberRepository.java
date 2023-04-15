package spring.study.entity.board.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface MemberRepository extends JpaRepository<Member, Long> {
    static final String update_member_last_logjn = "update Member set last_login_time = :lastLoginTime where email = :email";

    @Transactional
    @Modifying
    @Query(value = update_member_last_logjn, nativeQuery = true)
    public int updateMemberLastLogin(@Param("email") String email,
                                     @Param("lastLoginTime")LocalDateTime lastLoginTime);
    public Member findByEmail(String emfail);
}
