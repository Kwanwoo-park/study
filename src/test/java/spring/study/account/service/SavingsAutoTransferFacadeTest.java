package spring.study.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.facade.SavingsAutoTransferFacade;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.notification.service.NotificationService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;

class SavingsAutoTransferFacadeTest {
    private final AccountTransactionRepository transactionRepository = mock(AccountTransactionRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AccountTransactionService accountTransactionService = new AccountTransactionService(transactionRepository);
    private final SavingsAutoTransferFacade service = new SavingsAutoTransferFacade(
            accountService, notificationService, accountTransactionService
    );

    private Member member;
    private Account source;
    private Account savings;
    private LocalDate dueDate;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).email("member@test.com").build();
        source = Account.builder()
                .account("9191000")
                .amount(200_000L)
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        savings = Account.builder()
                .account("9192000")
                .amount(0L)
                .name("savings")
                .accountType(AccountType.INSTALLMENT_SAVINGS)
                .member(member)
                .build();
        dueDate = LocalDate.of(2026, 8, 10);
        savings.setMaturityAt(LocalDateTime.of(2027, 7, 10, 0, 0));
        savings.configureSavingsAutoTransfer(source, 100_000L, 10, dueDate.minusMonths(1));
        when(accountService.findSavingsSourceAccountNumber(savings.getAccount())).thenReturn(source.getAccount());
        when(accountService.findByAccountForUpdate(savings.getAccount())).thenReturn(savings);
        when(accountService.findByAccountForUpdate(source.getAccount())).thenReturn(source);
        when(transactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void duePaymentShouldTransferAndAdvanceToNextMonth() {
        service.processDuePayment(savings.getAccount(), dueDate, dueDate.atTime(9, 0));

        assertEquals(100_000L, source.getAmount());
        assertEquals(100_000L, savings.getAmount());
        assertEquals(LocalDate.of(2026, 9, 10), savings.getNextSavingsPaymentDate());
        assertNull(savings.getLastSavingsFailureNotificationDate());
        ArgumentCaptor<AccountTransaction> captor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertEquals(AccountTransactionType.SAVINGS_PAYMENT, captor.getValue().getTransactionType());
        var locks = inOrder(accountService);
        locks.verify(accountService).findByAccountForUpdate(source.getAccount());
        locks.verify(accountService).findByAccountForUpdate(savings.getAccount());
    }

    @Test
    void threeDaysOverdueTerminatesEvenWhenTheSourceNowHasEnoughMoney() {
        LocalDateTime time = dueDate.plusDays(3).atTime(9, 0);
        when(accountService.terminateInterestAccount(savings, source, member, time))
                .thenReturn(new AccountSettlementResult(savings.getAccount(), source.getAccount(), 0L, 0L, 0L, false));

        service.processDuePayment(savings.getAccount(), time.toLocalDate(), time);

        verify(accountService).terminateInterestAccount(savings, source, member, time);
        assertEquals(200_000L, source.getAmount());
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                transaction -> transaction.getTransactionType() == AccountTransactionType.SAVINGS_PAYMENT));
    }

    @Test
    void paymentDayShouldUseMonthEndAndReturnToConfiguredDay() {
        savings.setMaturityAt(LocalDateTime.of(2026, 1, 31, 0, 0));
        savings.configureSavingsAutoTransfer(source, 100_000L, 31, LocalDate.of(2025, 1, 1));

        assertEquals(LocalDate.of(2025, 1, 31), savings.getNextSavingsPaymentDate());
        savings.completeSavingsPayment();
        assertEquals(LocalDate.of(2025, 2, 28), savings.getNextSavingsPaymentDate());
        savings.completeSavingsPayment();
        assertEquals(LocalDate.of(2025, 3, 31), savings.getNextSavingsPaymentDate());
    }

    @Test
    void paymentDayShouldUseLeapDayInLeapYear() {
        savings.setMaturityAt(LocalDateTime.of(2025, 1, 31, 0, 0));
        savings.configureSavingsAutoTransfer(source, 100_000L, 31, LocalDate.of(2024, 1, 1));

        savings.completeSavingsPayment();

        assertEquals(LocalDate.of(2024, 2, 29), savings.getNextSavingsPaymentDate());
    }

    @Test
    void initialPaymentShouldScheduleConfiguredDayInTheFollowingMonth() {
        savings.setMaturityAt(LocalDateTime.of(2027, 8, 23, 0, 0));
        savings.configureSavingsAutoTransfer(source, 100_000L, 31, LocalDate.of(2026, 8, 23));

        savings.completeInitialSavingsPayment(LocalDate.of(2026, 8, 23));

        assertEquals(LocalDate.of(2026, 9, 30), savings.getNextSavingsPaymentDate());
    }
}
