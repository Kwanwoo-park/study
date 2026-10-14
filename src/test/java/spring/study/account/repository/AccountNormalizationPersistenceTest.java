package spring.study.account.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.service.AccountService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY,
        connection = EmbeddedDatabaseConnection.H2
)
@Import(AccountService.class)
class AccountNormalizationPersistenceTest {
    @Autowired private AccountService accountService;
    @Autowired private AccountTransactionRepository transactions;
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void deletionAndWithdrawalKeepAccountTransactionHistory() {
        Member owner = owner();
        Account checking = account("9191000", AccountType.DEPOSIT_WITHDRAWAL);
        checking.setMember(owner);
        accountRepository.saveAndFlush(checking);
        AccountTransaction history = transactions.saveAndFlush(AccountTransaction.builder()
                .depositAccount(checking).transactionType(AccountTransactionType.DEPOSIT)
                .transactionStatus(AccountTransactionStatus.COMPLETED).amount(10000L)
                .transactionTime(LocalDateTime.now()).build());

        accountService.deleteByAccount(checking.getAccount());
        accountService.deleteByMember(owner);
        entityManager.remove(owner);
        entityManager.flush();
        entityManager.clear();

        Account saved = accountRepository.findById(checking.getAccount()).orElseThrow();
        assertEquals(AccountStatus.DELETED, saved.getAccountStatus());
        assertNull(saved.getMember());
        assertEquals(saved.getAccount(), transactions.findById(history.getId()).orElseThrow().getDepositAccount().getAccount());
    }

    @Test
    void withdrawalRequiresZeroBalancesAndTerminatedInterestAccounts() {
        Member owner = owner();
        Account checking = account("9191000", AccountType.DEPOSIT_WITHDRAWAL);
        checking.setMember(owner);
        checking.setAmount(10000L);
        accountRepository.saveAndFlush(checking);
        assertThrows(spring.study.common.exception.BusinessStateException.class, () -> accountService.deleteByMember(owner));
        checking.setAmount(0L);
        Account savings = account("9192000", AccountType.INSTALLMENT_SAVINGS);
        savings.setMember(owner);
        accountRepository.saveAndFlush(savings);
        assertThrows(spring.study.common.exception.BusinessStateException.class, () -> accountService.deleteByMember(owner));
    }

    private Member owner() {
        return entityManager.persistAndFlush(Member.builder().email("owner@example.test").pwd("hash").name("owner")
                .role(Role.USER).phone("010-1111-2222").birth("2000-01-01").profile("profile.png").build());
    }

    @Test
    void checkingAccountShouldNotCreateSubtypeRows() {
        Account checking = account("9191000", AccountType.DEPOSIT_WITHDRAWAL);

        accountRepository.saveAndFlush(checking);
        entityManager.clear();

        Account saved = accountRepository.findById(checking.getAccount()).orElseThrow();
        assertNull(saved.getInterestDetail());
        assertNull(saved.getSavingsAutoTransfer());
    }

    @Test
    void savingsShouldPersistInterestAndAutoTransferInSeparateRows() {
        Account checking = accountRepository.saveAndFlush(account("9191000", AccountType.DEPOSIT_WITHDRAWAL));
        Account savings = account("9192000", AccountType.INSTALLMENT_SAVINGS);
        savings.configureSavingsAutoTransfer(checking, 100_000L, 31, LocalDate.of(2026, 1, 1));

        accountRepository.saveAndFlush(savings);
        entityManager.clear();

        Account saved = accountRepository.findById(savings.getAccount()).orElseThrow();
        assertNotNull(saved.getInterestDetail());
        assertNotNull(saved.getSavingsAutoTransfer());
        assertEquals(checking.getAccount(), saved.getSavingsSourceAccount().getAccount());
        assertEquals(100_000L, saved.getMonthlySavingsAmount());
        assertEquals(LocalDate.of(2026, 1, 31), saved.getNextSavingsPaymentDate());
        assertEquals(
                List.of(saved.getAccount()),
                accountRepository.findDueSavingsAccountNumbers(
                        AccountType.INSTALLMENT_SAVINGS,
                        List.of(AccountStatus.ACTIVE),
                        LocalDate.of(2026, 1, 31)
                )
        );
    }

    private Account account(String number, AccountType accountType) {
        return Account.builder()
                .account(number)
                .amount(0L)
                .name(number)
                .accountType(accountType)
                .build();
    }
}
