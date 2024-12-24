package spring.study.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    public Member findByEmail(String email);

    public Page<Member> findByName(String name, Pageable pageable);

    public List<Member> findByName(String name);

    public Member findByPhoneAndBirth(String phone, String birth);
}
