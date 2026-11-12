package spring.study.account.service;

import org.junit.jupiter.api.Test;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;
import spring.study.account.repository.AccountRepository;
import spring.study.member.entity.Member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

class AccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountService accountService = new AccountService(accountRepository);

    @Test
    void savingsAccountCreationShouldFailWhenSourceBalanceIsInsufficient() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account checking = Account.builder()
                .account("9191000")
                .amount(50_000L)
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(true);
        when(accountRepository.findByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(List.of(checking));
        when(accountRepository.findByAccount(checking.getAccount()))
                .thenReturn(java.util.Optional.of(checking));
        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.INSTALLMENT_SAVINGS);
        request.setMonthlySavingsAmount(100_000L);
        request.setMonthlySavingsDay(15);
        request.setAutoTerminationAcknowledged(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(member, request, checking)
        );

        assertEquals("선택한 입출금 계좌의 잔액이 적금 납입 금액보다 부족합니다", exception.getMessage());
        assertEquals(50_000L, checking.getAmount());
        verify(accountRepository, never()).save(any(Account.class));
    }

    private Account account(String number, long amount, AccountType type, Member member) {
        return Account.builder()
                .account(number)
                .amount(amount)
                .name(number)
                .accountType(type)
                .member(member)
                .build();
    }

    private void mockLockedAccounts(Account first, Account second) {
        when(accountRepository.findByAccount(first.getAccount()))
                .thenReturn(java.util.Optional.of(first));
        when(accountRepository.findByAccount(second.getAccount()))
                .thenReturn(java.util.Optional.of(second));
    }

    private AccountTranDto transferRequest(Account withdrawal, Account deposit, long amount) {
        AccountTranDto dto = new AccountTranDto();
        dto.setAccount(withdrawal.getAccount());
        dto.setTranAccount(deposit.getAccount());
        dto.setAmount(amount);
        return dto;
    }
}
