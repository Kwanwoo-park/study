package spring.study.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@NoArgsConstructor
@Entity(name = "savingsAutoTransfer")
@Table(name = "savings_auto_transfer")
public class SavingsAutoTransfer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "savings_account")
    private String savingsAccountNumber;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_account")
    private Account savingsAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account")
    private Account sourceAccount;

    @NotNull
    @Column(name = "monthly_amount", nullable = false)
    private Long monthlyAmount;

    @NotNull
    @Column(name = "payment_day", nullable = false)
    private Integer paymentDay;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "last_failure_notification_date")
    private LocalDate lastFailureNotificationDate;

    static SavingsAutoTransfer create(Account savingsAccount,
                                      Account sourceAccount,
                                      long monthlyAmount,
                                      int paymentDay,
                                      LocalDate configuredDate) {
        SavingsAutoTransfer transfer = new SavingsAutoTransfer();
        transfer.savingsAccount = savingsAccount;
        transfer.sourceAccount = sourceAccount;
        transfer.monthlyAmount = monthlyAmount;
        transfer.paymentDay = paymentDay;
        transfer.nextPaymentDate = calculateNextPaymentDate(configuredDate, paymentDay);
        return transfer;
    }

    void completePayment(LocalDateTime maturityAt) {
        if (nextPaymentDate == null) {
            return;
        }

        YearMonth nextMonth = YearMonth.from(nextPaymentDate).plusMonths(1L);
        LocalDate nextDate = nextMonth.atDay(Math.min(paymentDay, nextMonth.lengthOfMonth()));
        nextPaymentDate = maturityAt != null && nextDate.isAfter(maturityAt.toLocalDate())
                ? null
                : nextDate;
        lastFailureNotificationDate = null;
    }

    void recordFailureNotification(LocalDate notificationDate) {
        lastFailureNotificationDate = notificationDate;
    }

    void disable() {
        sourceAccount = null;
        nextPaymentDate = null;
        lastFailureNotificationDate = null;
    }

    boolean isConfigured() {
        return sourceAccount != null
                && monthlyAmount != null
                && paymentDay != null
                && nextPaymentDate != null;
    }

    private static LocalDate calculateNextPaymentDate(LocalDate configuredDate, int paymentDay) {
        YearMonth currentMonth = YearMonth.from(configuredDate);
        LocalDate candidate = currentMonth.atDay(Math.min(paymentDay, currentMonth.lengthOfMonth()));
        if (!candidate.isAfter(configuredDate)) {
            YearMonth nextMonth = currentMonth.plusMonths(1L);
            candidate = nextMonth.atDay(Math.min(paymentDay, nextMonth.lengthOfMonth()));
        }
        return candidate;
    }
}
