package spring.study.account.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountType;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByMember(Member member);

    List<Account> findByMemberAndAccountStatus(Member member, AccountStatus accountStatus);

    boolean existsByMemberAndAccountTypeAndAccountStatus(Member member,
                                                          AccountType accountType,
                                                          AccountStatus accountStatus);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from account a where a.account = :account")
    Optional<Account> findByAccountForUpdate(@Param("account") String account);

    List<Account> findByAccountStatusAndMaturityAtLessThanEqual(AccountStatus accountStatus,
                                                                 LocalDateTime maturityAt);

    @Transactional
    void deleteByMember(Member member);
}
