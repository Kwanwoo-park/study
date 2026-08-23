package spring.study.account.service;

import org.junit.jupiter.api.Test;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountTransactionServiceTest {

    private final AccountService accountService = mock(AccountService.class);
    private final AccountTransactionRepository accountTransactionRepository = mock(AccountTransactionRepository.class);
    private final AccountTransactionService accountTransactionService = new AccountTransactionService(
            accountService,
            accountTransactionRepository
    );

    @Test
    void cancelTransferShouldReverseBalancesAndCreateCancelTransaction() {
        Member sender = createMember(1L);
        Member receiver = createMember(2L);
        Account withdrawalAccount = createAccount("9191000", 10_000L, sender);
        Account depositAccount = createAccount("9192000", 10_000L, receiver);
        AccountTransaction transaction = createTransaction(
                1L,
                AccountTransactionType.TRANSFER,
                AccountTransactionStatus.COMPLETED,
                withdrawalAccount,
                depositAccount,
                LocalDateTime.now().minusHours(1)
        );

        when(accountTransactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));
        when(accountService.findByAccountForUpdate(withdrawalAccount.getAccount())).thenReturn(withdrawalAccount);
        when(accountService.findByAccountForUpdate(depositAccount.getAccount())).thenReturn(depositAccount);
        when(accountTransactionRepository.save(any(AccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountTransaction cancelTransaction = accountTransactionService.cancelTransaction(transaction.getId(), sender);

        assertThat(transaction.getTransactionStatus()).isEqualTo(AccountTransactionStatus.CANCELED);
        assertThat(withdrawalAccount.getAmount()).isEqualTo(20_000L);
        assertThat(depositAccount.getAmount()).isZero();
        assertThat(cancelTransaction.getTransactionType()).isEqualTo(AccountTransactionType.CANCEL);
        assertThat(cancelTransaction.getWithdrawalAccount()).isEqualTo(depositAccount);
        assertThat(cancelTransaction.getDepositAccount()).isEqualTo(withdrawalAccount);
        verify(accountService).notifyTransaction(transaction);
    }

    @Test
    void cancelShouldRejectTransactionOlderThanOneDay() {
        Member member = createMember(1L);
        Account account = createAccount("9191000", 10_000L, member);
        AccountTransaction transaction = createTransaction(
                1L,
                AccountTransactionType.DEPOSIT,
                AccountTransactionStatus.COMPLETED,
                null,
                account,
                LocalDateTime.now().minusDays(1).minusMinutes(1)
        );

        when(accountTransactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> accountTransactionService.cancelTransaction(transaction.getId(), member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("거래 후 하루가 지난 거래는 취소할 수 없습니다");
    }

    @Test
    void cancelShouldRejectWhenReversalWithdrawalAccountHasInsufficientBalance() {
        Member member = createMember(1L);
        Account account = createAccount("9191000", 5_000L, member);
        AccountTransaction transaction = createTransaction(
                1L,
                AccountTransactionType.DEPOSIT,
                AccountTransactionStatus.COMPLETED,
                null,
                account,
                LocalDateTime.now().minusHours(1)
        );

        when(accountTransactionRepository.findByIdForUpdate(transaction.getId())).thenReturn(Optional.of(transaction));
        when(accountService.findByAccountForUpdate(account.getAccount())).thenReturn(account);

        assertThatThrownBy(() -> accountTransactionService.cancelTransaction(transaction.getId(), member))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("취소할 계좌의 잔액이 부족합니다");
    }

    @Test
    void transferRecipientCannotCancelIncomingTransfer() {
        Member sender = createMember(1L);
        Member receiver = createMember(2L);
        Account withdrawalAccount = createAccount("9191000", 10_000L, sender);
        Account depositAccount = createAccount("9192000", 10_000L, receiver);
        AccountTransaction transaction = createTransaction(1L, AccountTransactionType.TRANSFER,
                AccountTransactionStatus.COMPLETED, withdrawalAccount, depositAccount, LocalDateTime.now());
        when(accountTransactionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> accountTransactionService.cancelTransaction(1L, receiver))
                .isInstanceOf(SecurityException.class)
                .hasMessage("계좌이체는 보낸 사람만 취소할 수 있습니다");
        verify(accountService, never()).findByAccountForUpdate(any());
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    @Test
    void cancelShouldNotWithdrawMoneyFromSavingsAccount() {
        Member sender = createMember(1L);
        Member receiver = createMember(2L);
        Account withdrawalAccount = createAccount("9191000", 10_000L, sender);
        Account savingsAccount = Account.builder()
                .account("9192000")
                .amount(10_000L)
                .name("savings")
                .accountType(AccountType.INSTALLMENT_SAVINGS)
                .member(receiver)
                .build();
        AccountTransaction transaction = createTransaction(
                1L,
                AccountTransactionType.TRANSFER,
                AccountTransactionStatus.COMPLETED,
                withdrawalAccount,
                savingsAccount,
                LocalDateTime.now().minusHours(1)
        );
        when(accountTransactionRepository.findByIdForUpdate(transaction.getId()))
                .thenReturn(Optional.of(transaction));
        when(accountService.findByAccountForUpdate(withdrawalAccount.getAccount()))
                .thenReturn(withdrawalAccount);
        when(accountService.findByAccountForUpdate(savingsAccount.getAccount()))
                .thenReturn(savingsAccount);
        doThrow(new IllegalArgumentException("적금 계좌에서는 출금 또는 이체할 수 없습니다"))
                .when(accountService).validateOutgoingTransaction(savingsAccount);

        assertThatThrownBy(() -> accountTransactionService.cancelTransaction(transaction.getId(), sender))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("적금 계좌에서는 출금 또는 이체할 수 없습니다");
        assertThat(savingsAccount.getAmount()).isEqualTo(10_000L);
        assertThat(withdrawalAccount.getAmount()).isEqualTo(10_000L);
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("member" + id + "@test.com")
                .pwd("password")
                .name("member" + id)
                .role(Role.USER)
                .profile("profile.png")
                .phone("010-0000-000" + id)
                .birth("2000-01-01")
                .build();
    }

    private Account createAccount(String accountNumber, long amount, Member member) {
        return Account.builder()
                .account(accountNumber)
                .amount(amount)
                .name(accountNumber)
                .member(member)
                .build();
    }

    private AccountTransaction createTransaction(Long id, AccountTransactionType type, AccountTransactionStatus status, Account withdrawalAccount, Account depositAccount, LocalDateTime transactionTime) {
        return AccountTransaction.builder()
                .id(id)
                .transactionType(type)
                .transactionStatus(status)
                .amount(10_000L)
                .fee(0L)
                .withdrawalAccount(withdrawalAccount)
                .depositAccount(depositAccount)
                .balanceAfterTransaction(0L)
                .bankName("Kwanwoo site account")
                .transactionTime(transactionTime)
                .build();
    }
}
