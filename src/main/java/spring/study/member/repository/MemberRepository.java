package spring.study.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.entity.AccountStatus;

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

    Member findByPhoneAndBirth(String phone, String birth);

    Member findByRole(Role role);

    Boolean existsByEmail(String email);

    Boolean existsByPhone(String phone);

    List<Member> findByAccountStatusAndSuspendedUntilLessThanEqual(
            AccountStatus accountStatus,
            LocalDateTime suspendedUntil
    );
}
