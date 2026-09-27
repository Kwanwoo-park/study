package spring.study.account.service;

import org.junit.jupiter.api.Test;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.notification.service.NotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountInterestServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountTransactionRepository transactionRepository = mock(AccountTransactionRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AccountService accountService = new AccountService(accountRepository);

    @Test
    void installmentSavingsShouldAccrueFivePercentForOneYear() {
        LocalDateTime openedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        Account account = interestAccount("savings", 1_000_000L, AccountType.INSTALLMENT_SAVINGS, openedAt);

        long interest = won(account.calculateAccruedInterestAt(openedAt.plusYears(1)));

        assertEquals(50_000L, interest);
    }

    @Test
    void timeDepositShouldAccrueThreePointFivePercentForOneYear() {
        LocalDateTime openedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        Account account = interestAccount("deposit", 1_000_000L, AccountType.TIME_DEPOSIT, openedAt);

        long interest = won(account.calculateAccruedInterestAt(openedAt.plusYears(1)));

        assertEquals(35_000L, interest);
    }

    @Test
    void laterInstallmentShouldOnlyEarnInterestFromItsDepositDate() {
        LocalDateTime openedAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        Account account = interestAccount("savings", 1_000_000L, AccountType.INSTALLMENT_SAVINGS, openedAt);
        LocalDateTime secondDepositAt = openedAt.plusDays(182);

        account.accrueInterestUntil(secondDepositAt);
        account.addAmount(1_000_000L);

        assertEquals(75_068L, won(account.calculateAccruedInterestAt(openedAt.plusYears(1))));
    }

    private Account interestAccount(String number, long amount, AccountType accountType, LocalDateTime openedAt) {
        Account account = Account.builder()
                .account(number)
                .amount(amount)
                .name(number)
                .accountType(accountType)
                .build();
        account.setOpenedAt(openedAt);
        account.setLastInterestCalculatedAt(openedAt);
        account.setMaturityAt(openedAt.plusYears(1));
        account.setAccruedInterest(BigDecimal.ZERO);
        account.setAccountStatus(AccountStatus.ACTIVE);
        return account;
    }

    private long won(BigDecimal interest) {
        return interest.setScale(0, RoundingMode.DOWN).longValue();
    }
}
