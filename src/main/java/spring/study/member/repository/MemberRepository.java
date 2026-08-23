package spring.study.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.entity.MemberStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByRegisterTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Member> findByEmail(String email);

    List<Member> findByEmailIn(Collection<String> emails);

    List<Member> findByNameContaining(String name);

    List<Member> findByIdIn(List<Long> idList);

    @Query("""
            select m.id
            from member m
            where m.id in :memberIds
              and (m.lastLoginTime is null or m.lastLoginTime <= :cutoff)
            """)
    List<Long> findInactiveMemberIds(@Param("memberIds") Collection<Long> memberIds,
                                     @Param("cutoff") LocalDateTime cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update member m set m.lastLoginTime = :accessedAt where m.id = :memberId")
    int updateLastLoginTime(@Param("memberId") Long memberId,
                            @Param("accessedAt") LocalDateTime accessedAt);

    Member findByPhoneAndBirth(String phone, String birth);

    Member findByRole(Role role);

    List<Member> findAllByRole(Role role);

    Boolean existsByEmail(String email);

    Boolean existsByPhone(String phone);

    List<Member> findByAccountStatusAndSuspendedUntilLessThanEqual(
            MemberStatus accountStatus,
            LocalDateTime suspendedUntil
    );
}
