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
import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByMember(Member member);

    List<Account> findByMemberAndAccountStatus(Member member, AccountStatus accountStatus);

    boolean existsByMemberAndAccountTypeAndAccountStatus(Member member,
                                                          AccountType accountType,
                                                          AccountStatus accountStatus);

    List<Account> findByMemberAndAccountTypeAndAccountStatus(Member member,
                                                              AccountType accountType,
                                                              AccountStatus accountStatus);

    @Query("""
            select a.account
            from account a
            join a.savingsAutoTransfer transfer
            where a.accountType = :accountType
              and a.accountStatus in :statuses
              and transfer.nextPaymentDate <= :processingDate
            """)
    List<String> findDueSavingsAccountNumbers(@Param("accountType") AccountType accountType,
                                               @Param("statuses") Collection<AccountStatus> statuses,
                                               @Param("processingDate") LocalDate processingDate);

    @Query("""
            select case when count(a) > 0 then true else false end
            from account a
            join a.savingsAutoTransfer transfer
            where transfer.sourceAccount = :sourceAccount
              and a.accountStatus in :statuses
            """)
    boolean existsBySavingsSourceAccountAndAccountStatusIn(
            @Param("sourceAccount") Account savingsSourceAccount,
            @Param("statuses") Collection<AccountStatus> accountStatuses
    );

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from account a where a.account = :account")
    Optional<Account> findByAccountForUpdate(@Param("account") String account);

    @Query("""
            select a
            from account a
            join a.interestDetail interest
            where a.accountStatus = :accountStatus
              and interest.maturityAt <= :maturityAt
            """)
    List<Account> findByAccountStatusAndMaturityAtLessThanEqual(
            @Param("accountStatus") AccountStatus accountStatus,
            @Param("maturityAt") LocalDateTime maturityAt
    );

    @Transactional
    void deleteByMember(Member member);
}
