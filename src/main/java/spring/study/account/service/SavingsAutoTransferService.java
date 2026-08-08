package spring.study.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SavingsAutoTransferService {
    private static final int PAYMENT_GRACE_DAYS = 3;

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final AccountService accountService;
    private final NotificationService notificationService;

    public List<String> findDueSavingsAccountNumbers(LocalDate processingDate) {
        return accountRepository.findDueSavingsAccountNumbers(
                AccountType.INSTALLMENT_SAVINGS,
                List.of(AccountStatus.ACTIVE, AccountStatus.MATURED),
                processingDate
        );
    }

    @Transactional
    public void processDuePayment(String savingsAccountNumber,
                                  LocalDate processingDate,
                                  LocalDateTime processingTime) {
        Account savings = accountRepository.findByAccountForUpdate(savingsAccountNumber).orElse(null);
        if (!isDue(savings, processingDate)) {
            return;
        }

        Account source = savings.getSavingsSourceAccount();
        if (source == null) {
            return;
        }
        source = accountRepository.findByAccountForUpdate(source.getAccount()).orElse(null);
        if (!isValidSource(savings, source)) {
            return;
        }

        long paymentAmount = savings.getMonthlySavingsAmount();
        if (source.getAmount() >= paymentAmount) {
            transferPayment(savings, source, paymentAmount, processingTime);
            return;
        }

        LocalDate dueDate = savings.getNextSavingsPaymentDate();
        LocalDate terminationDate = dueDate.plusDays(PAYMENT_GRACE_DAYS);
        if (!processingDate.isBefore(terminationDate)) {
            accountService.terminateInterestAccount(
                    savings.getAccount(),
                    source.getAccount(),
                    savings.getMember()
            );
            notificationService.createNotification(
                    savings.getMember(),
                    savings.getName() + " 계좌가 자동이체일로부터 3일 동안 미납되어 자동 해지되었습니다.",
                    Group.TRAN,
                    savings.getAccount()
            );
            return;
        }

        if (!processingDate.equals(savings.getLastSavingsFailureNotificationDate())) {
            savings.recordSavingsFailureNotification(processingDate);
            String amount = NumberFormat.getNumberInstance(Locale.KOREA).format(paymentAmount);
            notificationService.createNotification(
                    savings.getMember(),
                    savings.getName() + " 계좌의 " + amount + "원 자동이체가 잔액 부족으로 실패했습니다. "
                            + terminationDate + "까지 납입되지 않으면 자동 해지됩니다.",
                    Group.TRAN,
                    savings.getAccount()
            );
        }
    }

    private boolean isDue(Account savings, LocalDate processingDate) {
        return savings != null
                && savings.isSavingsAutoTransferConfigured()
                && savings.getAccountStatus() != AccountStatus.TERMINATED
                && !savings.getNextSavingsPaymentDate().isAfter(processingDate);
    }

    private boolean isValidSource(Account savings, Account source) {
        return source != null
                && source.getAccountType() == AccountType.DEPOSIT_WITHDRAWAL
                && source.getAccountStatus() == AccountStatus.ACTIVE
                && source.getMember() != null
                && savings.getMember() != null
                && source.getMember().getId().equals(savings.getMember().getId());
    }

    private void transferPayment(Account savings,
                                 Account source,
                                 long paymentAmount,
                                 LocalDateTime processingTime) {
        savings.accrueInterestUntil(processingTime);
        source.subAmount(paymentAmount);
        savings.addAmount(paymentAmount);

        AccountTransaction transaction = accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(AccountTransactionType.SAVINGS_PAYMENT)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(paymentAmount)
                .fee(0L)
                .withdrawalAccount(source)
                .depositAccount(savings)
                .balanceAfterTransaction(source.getAmount())
                .memo("적금 정기 자동이체")
                .counterpartyName(savings.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(processingTime)
                .build());
        savings.completeSavingsPayment();
        accountService.notifyTransaction(transaction);
    }
}
