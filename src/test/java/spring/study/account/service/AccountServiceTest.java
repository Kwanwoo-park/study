package spring.study.account.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.account.dto.AccountTranDto;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

class AccountServiceTest {
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountService accountService = new AccountService(accountRepository);

    @Test
    void createAccountShouldPersistSelectedAccountType() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(true);
        Account checking = Account.builder()
                .account("9191000")
                .amount(200_000L)
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        when(accountRepository.findByMemberAndAccountTypeAndAccountStatus(
                member, AccountType.DEPOSIT_WITHDRAWAL, AccountStatus.ACTIVE
        )).thenReturn(List.of(checking));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findByAccountForUpdate(checking.getAccount()))
                .thenReturn(java.util.Optional.of(checking));

        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.INSTALLMENT_SAVINGS);
        request.setMonthlySavingsAmount(100_000L);
        request.setMonthlySavingsDay(15);
        request.setAutoTerminationAcknowledged(true);
        Account account = accountService.createAccount(member, request, checking);

        assertEquals(AccountType.INSTALLMENT_SAVINGS, account.getAccountType());
        assertEquals("Kwanwoo site savings account", account.getName());
        AccountResponseDto response = new AccountResponseDto(account);
        assertEquals(false, response.isOutgoingTransferAllowed());
        assertEquals(checking.getAccount(), response.getSavingsSourceAccount());
        assertEquals(100_000L, response.getMonthlySavingsAmount());
        assertEquals(100_000L, checking.getAmount());
        assertEquals(100_000L, account.getAmount());
        YearMonth nextMonth = YearMonth.from(LocalDate.now()).plusMonths(1L);
        assertEquals(nextMonth.atDay(Math.min(15, nextMonth.lengthOfMonth())),
                account.getNextSavingsPaymentDate());
    }

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
        when(accountRepository.findByAccountForUpdate(checking.getAccount()))
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

        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(AccountType.TIME_DEPOSIT);
        request.setTimeDepositAmount(400_000L);
        request.setMaturityMonths(18);

        Account account = accountService.createAccount(member, request, checking);

        assertEquals("Kwanwoo site time deposit account", account.getName());
        assertEquals(400_000L, account.getAmount());
        assertEquals(600_000L, checking.getAmount());
        assertEquals(18, account.getMaturityMonths());
        assertEquals(account.getOpenedAt().plusMonths(18), account.getMaturityAt());
        AccountResponseDto response = new AccountResponseDto(account);
        assertEquals(false, response.isOutgoingTransferAllowed());
        assertEquals(false, response.isDepositAllowed());
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
                () -> accountService.createAccount(member, request, null)
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
                () -> accountService.createAccount(member, request, checking)
        );

        assertEquals("선택한 입출금 계좌의 잔액이 예금 금액보다 부족합니다", exception.getMessage());
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
        when(accountRepository.findByAccountForUpdate(first.getAccount()))
                .thenReturn(java.util.Optional.of(first));
        when(accountRepository.findByAccountForUpdate(second.getAccount()))
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
