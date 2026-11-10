package spring.study.account.repository;

import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountType;
import spring.study.account.dto.AccountNumberProjection;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    @Transactional(readOnly = true)
    List<AccountNumberProjection> findByAccountTypeAndAccountStatusInAndSavingsAutoTransfer_NextPaymentDateLessThanEqual(AccountType accountType, Collection<AccountStatus> statuses, LocalDate processingDate);

    @Transactional(readOnly = true)
    boolean existsBySavingsAutoTransfer_SourceAccountAndAccountStatusIn(Account sourceAccount, Collection<AccountStatus> statuses);

    @Transactional(readOnly = true)
    List<Account> findByAccountStatusAndInterestDetail_MaturityAtLessThanEqual(AccountStatus accountStatus, LocalDateTime maturityAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(readOnly = true)
    List<Account> findByMemberOrderByAccount(Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional(readOnly = true)
    Optional<Account> findByAccount(String account);

    @Transactional(readOnly = true)
    List<Account> findByMember(Member member);

    @Transactional(readOnly = true)
    List<Account> findByMemberAndAccountStatus(Member member, AccountStatus accountStatus);

    @Transactional(readOnly = true)
    boolean existsByMemberAndAccountTypeAndAccountStatus(Member member, AccountType accountType, AccountStatus accountStatus);

    @Transactional(readOnly = true)
    List<Account> findByMemberAndAccountTypeAndAccountStatus(Member member, AccountType accountType, AccountStatus accountStatus);

    @Transactional
    void deleteByMember(Member member);
}
