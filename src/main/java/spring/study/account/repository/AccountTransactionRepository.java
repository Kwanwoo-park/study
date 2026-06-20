package spring.study.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
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
