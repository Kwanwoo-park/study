package spring.study.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountNotificationService {
    private final NotificationService notificationService;

    public void notifyTransaction(AccountTransaction transaction) {
        notifyMember(transaction.getWithdrawalAccount(), transaction);
        notifyMember(transaction.getDepositAccount(), transaction);
    }

    private void notifyMember(Account account, AccountTransaction transaction) {
        if (account == null || account.getMember() == null) return;
        String amount = NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.getAmount());
        String status = transaction.getTransactionStatus() == AccountTransactionStatus.CANCELED ? "취소" : "완료";
        String type = switch (transaction.getTransactionType()) {
            case TRANSFER -> "이체";
            case DEPOSIT -> "입금";
            case WITHDRAWAL -> "출금";
            case PAYMENT -> "결제";
            case REFUND -> "환불";
            case FEE -> "수수료";
            case CANCEL -> "취소";
            case INTEREST -> "이자";
            case TERMINATION -> "해지 정산";
            case SAVINGS_PAYMENT -> "적금 자동이체";
            case TIME_DEPOSIT_OPENING -> "예금 개설 입금";
        };
        notificationService.createNotification(account.getMember(),
                account.getName() + " 계좌에서 " + amount + "원 " + type + " 거래가 " + status + "되었습니다.",
                Group.TRAN, account.getAccount());
    }
}
