package spring.study.account.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionType;
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
import java.util.List;

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
        Account checking = Account.builder()
                .account("9191000")
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        when(accountRepository.findByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(List.of(checking));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.INSTALLMENT_SAVINGS);
        request.setMonthlySavingsAmount(100_000L);
        request.setMonthlySavingsDay(15);
        request.setAutoTerminationAcknowledged(true);
        Account account = accountService.createAccount(member, request);

        assertEquals(AccountType.INSTALLMENT_SAVINGS, account.getAccountType());
        assertEquals("Kwanwoo site savings account", account.getName());
        AccountResponseDto response = new AccountResponseDto(account);
        assertEquals("적금", response.getAccountTypeName());
        assertEquals(checking.getAccount(), response.getSavingsSourceAccount());
        assertEquals(100_000L, response.getMonthlySavingsAmount());
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
        Account checking = Account.builder()
                .account("9191000")
                .amount(1_000_000L)
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        when(accountRepository.findByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(List.of(checking));
        when(accountRepository.findByAccountForUpdate(checking.getAccount())).thenReturn(java.util.Optional.of(checking));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountTransactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.TIME_DEPOSIT);
        request.setTimeDepositAmount(400_000L);
        request.setMaturityMonths(18);

        Account account = accountService.createAccount(member, request);

        assertEquals("Kwanwoo site time deposit account", account.getName());
        assertEquals(400_000L, account.getAmount());
        assertEquals(600_000L, checking.getAmount());
        assertEquals(18, account.getMaturityMonths());
        assertEquals(account.getOpenedAt().plusMonths(18), account.getMaturityAt());
        ArgumentCaptor<AccountTransaction> transactionCaptor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(accountTransactionRepository).save(transactionCaptor.capture());
        assertEquals(AccountTransactionType.TIME_DEPOSIT_OPENING,
                transactionCaptor.getValue().getTransactionType());
    }

    @Test
    void timeDepositShouldRejectTermsLongerThanTwoYears() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(true);
        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.TIME_DEPOSIT);
        request.setTimeDepositAmount(100_000L);
        request.setMaturityMonths(25);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(member, request)
        );

        assertEquals("예금 만기 기간은 1개월부터 24개월까지 선택할 수 있습니다", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void timeDepositShouldRejectAnAmountGreaterThanTheLockedSourceBalance() {
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
        when(accountRepository.findByAccountForUpdate(checking.getAccount()))
                .thenReturn(java.util.Optional.of(checking));
        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.TIME_DEPOSIT);
        request.setTimeDepositAmount(100_000L);
        request.setMaturityMonths(12);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(member, request)
        );

        assertEquals("선택한 입출금 계좌의 잔액이 예금 금액보다 부족합니다", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
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
