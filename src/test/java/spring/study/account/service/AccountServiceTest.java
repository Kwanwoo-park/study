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
        when(accountTransactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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
        assertEquals(false, response.isOutgoingTransferAllowed());
        assertEquals(checking.getAccount(), response.getSavingsSourceAccount());
        assertEquals(100_000L, response.getMonthlySavingsAmount());
        assertEquals(100_000L, checking.getAmount());
        assertEquals(100_000L, account.getAmount());
        YearMonth nextMonth = YearMonth.from(LocalDate.now()).plusMonths(1L);
        assertEquals(nextMonth.atDay(Math.min(15, nextMonth.lengthOfMonth())),
                account.getNextSavingsPaymentDate());
        ArgumentCaptor<AccountTransaction> transactionCaptor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(accountTransactionRepository).save(transactionCaptor.capture());
        assertEquals(AccountTransactionType.SAVINGS_PAYMENT,
                transactionCaptor.getValue().getTransactionType());
        assertEquals("적금 개설 첫 납입", transactionCaptor.getValue().getMemo());
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
                () -> accountService.createAccount(member, request)
        );

        assertEquals("선택한 입출금 계좌의 잔액이 적금 납입 금액보다 부족합니다", exception.getMessage());
        assertEquals(50_000L, checking.getAmount());
        verify(accountRepository, never()).save(any(Account.class));
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    @Test
    void createAccountShouldDefaultToDepositWithdrawalType() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(member);

        assertEquals(AccountType.DEPOSIT_WITHDRAWAL, account.getAccountType());
        assertEquals("Kwanwoo site checking account", account.getName());
        assertEquals(true, new AccountResponseDto(account).isOutgoingTransferAllowed());
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
        AccountResponseDto response = new AccountResponseDto(account);
        assertEquals(false, response.isOutgoingTransferAllowed());
        assertEquals(false, response.isDepositAllowed());
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

    @Test
    void savingsAccountShouldNotBeUsedAsTransferWithdrawalAccount() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account savings = Account.builder()
                .account("9191000")
                .amount(20_000L)
                .name("savings")
                .accountType(AccountType.INSTALLMENT_SAVINGS)
                .member(member)
                .build();
        Account recipient = Account.builder()
                .account("9192000")
                .amount(0L)
                .name("recipient")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        AccountTranDto dto = transferRequest(savings, recipient, 10_000L);
        when(accountRepository.findByAccountForUpdate(savings.getAccount()))
                .thenReturn(java.util.Optional.of(savings));
        when(accountRepository.findByAccountForUpdate(recipient.getAccount()))
                .thenReturn(java.util.Optional.of(recipient));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.tranAccount(dto)
        );

        assertEquals("적금 계좌에서는 출금 또는 이체할 수 없습니다", exception.getMessage());
        assertEquals(20_000L, savings.getAmount());
        assertEquals(0L, recipient.getAmount());
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    @Test
    void savingsAccountShouldAcceptIncomingTransfer() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account checking = Account.builder()
                .account("9191000")
                .amount(20_000L)
                .name("checking")
                .accountType(AccountType.DEPOSIT_WITHDRAWAL)
                .member(member)
                .build();
        Account savings = Account.builder()
                .account("9192000")
                .amount(0L)
                .name("savings")
                .accountType(AccountType.INSTALLMENT_SAVINGS)
                .member(member)
                .build();
        AccountTranDto dto = transferRequest(checking, savings, 10_000L);
        when(accountRepository.findByAccountForUpdate(checking.getAccount()))
                .thenReturn(java.util.Optional.of(checking));
        when(accountRepository.findByAccountForUpdate(savings.getAccount()))
                .thenReturn(java.util.Optional.of(savings));
        when(accountTransactionRepository.save(any(AccountTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.tranAccount(dto);

        assertEquals(10_000L, checking.getAmount());
        assertEquals(10_000L, savings.getAmount());
        verify(accountTransactionRepository).save(any(AccountTransaction.class));
    }

    @Test
    void timeDepositShouldRejectOutgoingTransfer() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account timeDeposit = account("9191000", 20_000L, AccountType.TIME_DEPOSIT, member);
        Account checking = account("9192000", 0L, AccountType.DEPOSIT_WITHDRAWAL, member);
        AccountTranDto dto = transferRequest(timeDeposit, checking, 10_000L);
        mockLockedAccounts(timeDeposit, checking);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.tranAccount(dto)
        );

        assertEquals("예금 계좌에서는 입금, 출금 또는 이체할 수 없습니다", exception.getMessage());
        assertEquals(20_000L, timeDeposit.getAmount());
        assertEquals(0L, checking.getAmount());
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    @Test
    void timeDepositShouldRejectIncomingTransfer() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account checking = account("9191000", 20_000L, AccountType.DEPOSIT_WITHDRAWAL, member);
        Account timeDeposit = account("9192000", 0L, AccountType.TIME_DEPOSIT, member);
        AccountTranDto dto = transferRequest(checking, timeDeposit, 10_000L);
        mockLockedAccounts(checking, timeDeposit);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.tranAccount(dto)
        );

        assertEquals("예금 계좌에서는 입금, 출금 또는 이체할 수 없습니다", exception.getMessage());
        assertEquals(20_000L, checking.getAmount());
        assertEquals(0L, timeDeposit.getAmount());
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
    }

    @Test
    void timeDepositShouldRejectRegularDeposit() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        Account timeDeposit = account("9191000", 100_000L, AccountType.TIME_DEPOSIT, member);
        when(accountRepository.findById(timeDeposit.getAccount()))
                .thenReturn(java.util.Optional.of(timeDeposit));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit(timeDeposit.getAccount(), 10_000L)
        );

        assertEquals("예금 계좌에서는 입금, 출금 또는 이체할 수 없습니다", exception.getMessage());
        assertEquals(100_000L, timeDeposit.getAmount());
        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
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
