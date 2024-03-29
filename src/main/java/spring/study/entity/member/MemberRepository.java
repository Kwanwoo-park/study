package spring.study.entity.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    static final String update_member_last_login = "update member set last_login_time = :lastLoginTime where email = :email";
    static final String update_member_password = "update member set pwd = :password where email = :email";

    @Transactional
    @Modifying
    @Query(value = update_member_last_login, nativeQuery = true)
    public int updateMemberLastLogin(@Param("email") String email,
                                     @Param("lastLoginTime")LocalDateTime lastLoginTime);

    @Transactional
    @Modifying
    @Query(value = update_member_password, nativeQuery = true)
    public int updateMemberPassword(@Param("email") String email,
                                    @Param("password") String password);

    public Member findByEmail(String emfail);

    public List<Member> findByName(String name);
}
