package spring.study.account.service;

import org.junit.jupiter.api.Test;
import spring.study.account.dto.AccountTransactionResponseDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountTransactionServiceTest {

    private final AccountService accountService = mock(AccountService.class);
    private final AccountTransactionRepository accountTransactionRepository = mock(AccountTransactionRepository.class);
    private final AccountTransactionService accountTransactionService = new AccountTransactionService(
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

        var canceled = accountTransactionService.cancelTransaction(transaction, withdrawalAccount, depositAccount, transaction.getTransactionTime());
        AccountTransactionResponseDto cancelTransaction = new AccountTransactionResponseDto(canceled.cancellation());

        assertThat(transaction.getTransactionStatus()).isEqualTo(AccountTransactionStatus.CANCELED);
        assertThat(withdrawalAccount.getAmount()).isEqualTo(20_000L);
        assertThat(depositAccount.getAmount()).isZero();
        assertThat(cancelTransaction.getTransactionType()).isEqualTo(AccountTransactionType.CANCEL);
        assertThat(cancelTransaction.getWithdrawalAccount()).isEqualTo(depositAccount.getAccount());
        assertThat(cancelTransaction.getDepositAccount()).isEqualTo(withdrawalAccount.getAccount());
        assertThat(canceled.original()).isSameAs(transaction);
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
