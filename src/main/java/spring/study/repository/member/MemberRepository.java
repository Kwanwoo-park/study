package spring.study.repository.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.member.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Page<Member> findByName(String name, Pageable pageable);

    List<Member> findByName(String name);

    Member findByPhoneAndBirth(String phone, String birth);

    Boolean existsByEmail(String email);
}
