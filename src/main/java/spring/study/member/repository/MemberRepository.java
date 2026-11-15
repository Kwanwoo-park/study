package spring.study.member.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.entity.MemberStatus;
import spring.study.common.dto.EntityIdProjection;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    @Transactional(readOnly = true)
    List<Member> findByRegisterTimeBetween(LocalDateTime start, LocalDateTime end);

    @Transactional(readOnly = true)
    Optional<Member> findByEmail(String email);

    @Transactional(readOnly = true)
    List<Member> findByEmailIn(Collection<String> emails);

    @Transactional(readOnly = true)
    List<Member> findByNameContaining(String name);

    @Transactional(readOnly = true)
    List<Member> findByIdIn(List<Long> idList);

    // Restrict both OR branches to the candidate member IDs.
    @Transactional(readOnly = true)
    List<EntityIdProjection> findByIdInAndLastLoginTimeIsNullOrIdInAndLastLoginTimeLessThanEqual(Collection<Long> neverLoggedInMemberIds, Collection<Long> inactiveMemberIds, LocalDateTime cutoff);

    @Transactional(readOnly = true)
    Member findByPhoneAndBirth(String phone, String birth);

    @Transactional(readOnly = true)
    Member findByRole(Role role);

    @Transactional(readOnly = true)
    List<Member> findAllByRole(Role role);

    @Transactional(readOnly = true)
    Boolean existsByEmail(String email);

    @Transactional(readOnly = true)
    Boolean existsByPhone(String phone);

    @Transactional(readOnly = true)
    List<Member> findByAccountStatusAndSuspendedUntilLessThanEqual(MemberStatus accountStatus, LocalDateTime suspendedUntil);
}
