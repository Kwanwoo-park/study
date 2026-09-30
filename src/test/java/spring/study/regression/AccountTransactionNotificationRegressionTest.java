package spring.study.regression;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AccountTransactionNotificationRegressionTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountTransactionRepository accountTransactionRepository = mock(AccountTransactionRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);

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
