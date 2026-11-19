package spring.study.account.service;

import org.junit.jupiter.api.Test;
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

}
