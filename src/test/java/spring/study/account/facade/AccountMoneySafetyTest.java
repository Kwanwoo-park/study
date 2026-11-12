package spring.study.account.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.service.AccountService;
import spring.study.account.service.AccountTransactionService;
import spring.study.common.component.TransactionRunner;
import spring.study.member.entity.Member;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountMoneySafetyTest {
    private final AccountRepository repository = mock(AccountRepository.class);
    private final AccountTransactionService transactionService = mock(AccountTransactionService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final TransactionRunner transactions = mock(TransactionRunner.class);
    private final AccountFacade facade = new AccountFacade(new AccountService(repository), transactionService, notifications, transactions);

    @BeforeEach
    void setUp() {
        when(transactions.write(any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"INSTALLMENT_SAVINGS", "TIME_DEPOSIT"})
    void interestBearingAccountsCannotBeUsedAsTransferSources(AccountType type) {
        Member owner = Member.builder().id(1L).build();
        Account source = account("100", owner, type, 200_000L);
        Account target = account("200", owner, AccountType.DEPOSIT_WITHDRAWAL, 0L);
        when(repository.findByAccount("100")).thenReturn(Optional.of(source));
        when(repository.findByAccount("200")).thenReturn(Optional.of(target));
        AccountTranDto request = new AccountTranDto();
        request.setAccount("100");
        request.setTranAccount("200");
        request.setAmount(10_000L);

        ResponseEntity<?> response = facade.tranAccount(request, owner);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(source.getAmount()).isEqualTo(200_000L);
        assertThat(target.getAmount()).isZero();
        verifyNoInteractions(transactionService, notifications);
    }

    @Test
    void transferReceiverCannotCancelTheSendersTransaction() {
        Member sender = Member.builder().id(1L).build();
        Member receiver = Member.builder().id(2L).build();
        Account source = account("100", sender, AccountType.DEPOSIT_WITHDRAWAL, 90_000L);
        Account target = account("200", receiver, AccountType.DEPOSIT_WITHDRAWAL, 10_000L);
        AccountTransaction transfer = AccountTransaction.builder().id(1L).withdrawalAccount(source).depositAccount(target)
                .transactionType(AccountTransactionType.TRANSFER).transactionStatus(AccountTransactionStatus.COMPLETED)
                .transactionTime(LocalDateTime.now()).amount(10_000L).build();
        when(transactionService.findUpdate(1L)).thenReturn(transfer);

        assertThat(facade.cancel(1L, receiver).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(transfer.getTransactionStatus()).isEqualTo(AccountTransactionStatus.COMPLETED);
        assertThat(source.getAmount()).isEqualTo(90_000L);
        assertThat(target.getAmount()).isEqualTo(10_000L);
        verifyNoInteractions(repository, notifications);
        verify(transactionService, never()).cancelTransaction(any(), any(), any(), any());
    }

    private Account account(String number, Member owner, AccountType type, long amount) {
        return Account.builder().account(number).member(owner).accountType(type).amount(amount).name(number).build();
    }
}
