package spring.study.account.dto;

import org.junit.jupiter.api.Test;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTransactionResponseDtoTest {

    @Test
    void transferShouldBeCancelableOnlyBySender() {
        Member sender = member(1L);
        Member receiver = member(2L);
        AccountTransaction transaction = transaction(
                AccountTransactionType.TRANSFER,
                account("9191000", sender),
                account("9192000", receiver)
        );

        assertThat(new AccountTransactionResponseDto(transaction, sender).isCancelable()).isTrue();
        assertThat(new AccountTransactionResponseDto(transaction, receiver).isCancelable()).isFalse();
    }

    @Test
    void regularDepositShouldRemainCancelableByDepositAccountOwner() {
        Member owner = member(1L);
        AccountTransaction transaction = transaction(
                AccountTransactionType.DEPOSIT,
                null,
                account("9191000", owner)
        );

        assertThat(new AccountTransactionResponseDto(transaction, owner).isCancelable()).isTrue();
    }

    private AccountTransaction transaction(AccountTransactionType type, Account withdrawal, Account deposit) {
        return AccountTransaction.builder()
                .id(1L)
                .transactionType(type)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(10_000L)
                .withdrawalAccount(withdrawal)
                .depositAccount(deposit)
                .transactionTime(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    private Account account(String accountNumber, Member member) {
        return Account.builder()
                .account(accountNumber)
                .amount(10_000L)
                .name(accountNumber)
                .member(member)
                .build();
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .email("member" + id + "@test.com")
                .name("member" + id)
                .build();
    }
}
