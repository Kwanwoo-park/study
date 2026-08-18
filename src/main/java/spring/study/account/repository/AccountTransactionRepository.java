package spring.study.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountTransaction t left join fetch t.withdrawalAccount left join fetch t.depositAccount where t.id = :id")
    Optional<AccountTransaction> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"withdrawalAccount", "depositAccount"})
    Page<AccountTransaction> findByWithdrawalAccountOrDepositAccount(
            Account withdrawalAccount,
            Account depositAccount,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"withdrawalAccount", "depositAccount"})
    Page<AccountTransaction> findByWithdrawalAccount(Account withdrawalAccount, Pageable pageable);

    @EntityGraph(attributePaths = {"withdrawalAccount", "depositAccount"})
    Page<AccountTransaction> findByDepositAccount(Account depositAccount, Pageable pageable);
}
