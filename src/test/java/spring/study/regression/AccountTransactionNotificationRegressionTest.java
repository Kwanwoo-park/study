package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.account.service.AccountService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountTransactionNotificationRegressionTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountTransactionRepository accountTransactionRepository = mock(AccountTransactionRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AccountService accountService = new AccountService(
            accountRepository,
            accountTransactionRepository,
            notificationService
    );

    @Test
    void depositShouldCreateTransactionNotification() {
        Member member = createMember(1L, "deposit@test.com", "입금회원");
        Account account = createAccount("9191000", 0L, "입금계좌", member);

        when(accountRepository.findById(account.getAccount())).thenReturn(Optional.of(account));
        when(accountTransactionRepository.save(any(AccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.deposit(account.getAccount(), 10_000L);

        verify(notificationService).createNotification(
                member,
                "입금계좌 계좌에서 10,000원 입금 거래가 완료되었습니다.",
                Group.TRAN,
                account.getAccount()
        );
    }

    @Test
    void transferShouldCreateWithdrawalAndDepositNotifications() {
        Member sender = createMember(1L, "sender@test.com", "보내는회원");
        Member receiver = createMember(2L, "receiver@test.com", "받는회원");
        Account withdrawalAccount = createAccount("9191000", 20_000L, "출금계좌", sender);
        Account depositAccount = createAccount("9192000", 0L, "입금계좌", receiver);
        AccountTranDto dto = new AccountTranDto();
        dto.setAccount(withdrawalAccount.getAccount());
        dto.setTranAccount(depositAccount.getAccount());
        dto.setAmount(10_000L);

        when(accountRepository.findById(withdrawalAccount.getAccount())).thenReturn(Optional.of(withdrawalAccount));
        when(accountRepository.findById(depositAccount.getAccount())).thenReturn(Optional.of(depositAccount));
        when(accountTransactionRepository.save(any(AccountTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.tranAccount(dto);

        verify(notificationService).createNotification(
                sender,
                "출금계좌 계좌에서 10,000원 이체 거래가 완료되었습니다.",
                Group.TRAN,
                withdrawalAccount.getAccount()
        );
        verify(notificationService).createNotification(
                receiver,
                "입금계좌 계좌에서 10,000원 이체 거래가 완료되었습니다.",
                Group.TRAN,
                depositAccount.getAccount()
        );
    }

    @Test
    void canceledTransactionNotificationShouldUseCanceledMessage() throws Exception {
        Member member = createMember(1L, "cancel@test.com", "취소회원");
        Account account = createAccount("9191000", 10_000L, "취소계좌", member);
        AccountTransaction transaction = AccountTransaction.builder()
                .transactionType(AccountTransactionType.DEPOSIT)
                .transactionStatus(AccountTransactionStatus.CANCELED)
                .amount(10_000L)
                .fee(0L)
                .depositAccount(account)
                .balanceAfterTransaction(account.getAmount())
                .bankName("Kwanwoo site account")
                .transactionTime(LocalDateTime.now())
                .build();

        Method method = AccountService.class.getDeclaredMethod("notifyTransaction", AccountTransaction.class);
        method.setAccessible(true);
        method.invoke(accountService, transaction);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createNotification(
                any(Member.class),
                messageCaptor.capture(),
                any(Group.class),
                any(String.class)
        );
        assertThat(messageCaptor.getValue()).contains("거래가 취소되었습니다.");
    }

    private Member createMember(Long id, String email, String name) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("password")
                .name(name)
                .role(Role.USER)
                .profile("profile.png")
                .phone("010-0000-0000")
                .birth("2000-01-01")
                .build();
    }

    private Account createAccount(String accountNumber, long amount, String name, Member member) {
        return Account.builder()
                .account(accountNumber)
                .amount(amount)
                .name(name)
                .member(member)
                .build();
    }
}
