package spring.study.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "accountInterest")
@Table(name = "account_interest")
public class AccountInterest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "account")
    private String accountNumber;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account")
    private Account account;

    @NotNull
    @Column(name = "annual_interest_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal annualInterestRate;

    @NotNull
    @Column(name = "maturity_at", nullable = false)
    private LocalDateTime maturityAt;

    @NotNull
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @NotNull
    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @NotNull
    @Column(name = "accrued_interest", nullable = false, precision = 24, scale = 10)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_amount", nullable = false)
    private long paidAmount;

    static AccountInterest create(Account account,
                                  BigDecimal annualInterestRate,
                                  LocalDateTime openedAt) {
        AccountInterest interest = new AccountInterest();
        interest.account = account;
        interest.annualInterestRate = annualInterestRate;
        interest.maturityAt = openedAt.plusYears(1L);
        interest.termMonths = 12;
        interest.lastCalculatedAt = openedAt;
        interest.accruedInterest = BigDecimal.ZERO;
        return interest;
    }

    BigDecimal calculateAccruedInterestAt(long principal,
                                          AccountStatus accountStatus,
                                          LocalDateTime calculationTime) {
        BigDecimal accumulated = accruedInterest == null ? BigDecimal.ZERO : accruedInterest;
        if (accountStatus == AccountStatus.TERMINATED) {
            return accumulated;
        }

        LocalDateTime end = calculationTime.isAfter(maturityAt) ? maturityAt : calculationTime;
        if (!end.isAfter(lastCalculatedAt) || principal <= 0L) {
            return accumulated;
        }

        long seconds = Duration.between(lastCalculatedAt, end).getSeconds();
        BigDecimal pending = BigDecimal.valueOf(principal)
                .multiply(annualInterestRate)
                .multiply(BigDecimal.valueOf(seconds))
                .divide(BigDecimal.valueOf(365L * 24L * 60L * 60L), 10, RoundingMode.HALF_UP);
        return accumulated.add(pending);
    }

    void accrueUntil(long principal,
                     AccountStatus accountStatus,
                     LocalDateTime calculationTime) {
        LocalDateTime end = calculationTime.isAfter(maturityAt) ? maturityAt : calculationTime;
        accruedInterest = calculateAccruedInterestAt(principal, accountStatus, calculationTime);
        if (end.isAfter(lastCalculatedAt)) {
            lastCalculatedAt = end;
        }
    }

    long settle(LocalDateTime settlementTime) {
        long payableInterest = accruedInterest.setScale(0, RoundingMode.DOWN).longValue();
        paidAt = settlementTime;
        paidAmount = payableInterest;
        return payableInterest;
    }

    void configureTerm(LocalDateTime openedAt, int termMonths) {
        this.termMonths = termMonths;
        this.maturityAt = openedAt.plusMonths(termMonths);
    }
}
