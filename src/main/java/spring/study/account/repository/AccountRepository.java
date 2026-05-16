package spring.study.account.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.account.entity.Account;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByMember(Member member);

    @Transactional
    void deleteByMember(Member member);
}
