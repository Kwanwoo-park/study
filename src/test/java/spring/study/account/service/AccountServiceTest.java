package spring.study.account.service;

import org.junit.jupiter.api.Test;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.notification.service.NotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountTransactionRepository accountTransactionRepository = mock(AccountTransactionRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AccountService accountService = new AccountService(
            accountRepository,
            accountTransactionRepository,
            notificationService
    );

    @Test
    void createAccountShouldPersistSelectedAccountType() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(member, AccountType.INSTALLMENT_SAVINGS);

        assertEquals(AccountType.INSTALLMENT_SAVINGS, account.getAccountType());
        assertEquals("Kwanwoo site savings account", account.getName());
        AccountResponseDto response = new AccountResponseDto(account);
        assertEquals("적금", response.getAccountTypeName());
    }

    @Test
    void createAccountShouldDefaultToDepositWithdrawalType() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(member);

        assertEquals(AccountType.DEPOSIT_WITHDRAWAL, account.getAccountType());
        assertEquals("Kwanwoo site checking account", account.getName());
    }

    @Test
    void timeDepositAccountShouldUseItsTypeInTheDefaultName() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(member, AccountType.TIME_DEPOSIT);

        assertEquals("Kwanwoo site time deposit account", account.getName());
    }

    @Test
    void interestAccountShouldRequireAnActiveCheckingAccount() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(member, AccountType.INSTALLMENT_SAVINGS)
        );

        assertEquals("예적금 계좌를 만들려면 먼저 활성 상태의 입출금 계좌가 필요합니다", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }
}
