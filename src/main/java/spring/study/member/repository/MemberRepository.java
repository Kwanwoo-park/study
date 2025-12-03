package spring.study.member.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Page<Member> findByName(String name, Pageable pageable);

    List<Member> findByName(String name);

    Member findByPhoneAndBirth(String phone, String birth);

    Member findByRole(Role role);

    Boolean existsByEmail(String email);

    Boolean existsByPhone(String phone);
}
