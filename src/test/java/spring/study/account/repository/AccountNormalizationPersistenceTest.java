package spring.study.account.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountType;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
class AccountNormalizationPersistenceTest {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

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
