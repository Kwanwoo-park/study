package spring.study.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.member.entity.Member;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "account")
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 5L;

    @Id
    @Column(name = "account")
    private String account;

    @NotNull
    private long amount;

    @NotNull
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'DEPOSIT_WITHDRAWAL'")
    private AccountType accountType = AccountType.DEPOSIT_WITHDRAWAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'ACTIVE'")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @NotNull
    @Column(name = "annual_interest_rate", nullable = false, precision = 8, scale = 6,
            columnDefinition = "decimal(8,6) default 0")
    private BigDecimal annualInterestRate = BigDecimal.ZERO;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "maturity_at")
    private LocalDateTime maturityAt;

    @Column(name = "last_interest_calculated_at")
    private LocalDateTime lastInterestCalculatedAt;

    @NotNull
    @Column(name = "accrued_interest", nullable = false, precision = 24, scale = 10,
            columnDefinition = "decimal(24,10) default 0")
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "interest_paid_at")
    private LocalDateTime interestPaidAt;

    @Column(name = "interest_paid_amount", nullable = false, columnDefinition = "bigint default 0")
    private long interestPaidAmount;

    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Builder
    public Account(String account, long amount, String name, AccountType accountType, Member member) {
        this.account = account;
        this.amount = amount;
        this.name = name;
        this.accountType = accountType == null ? AccountType.DEPOSIT_WITHDRAWAL : accountType;
        this.accountStatus = AccountStatus.ACTIVE;
        this.annualInterestRate = this.accountType.getAnnualInterestRate();
        this.openedAt = LocalDateTime.now();
        this.lastInterestCalculatedAt = openedAt;
        this.maturityAt = this.accountType.isInterestBearing() ? openedAt.plusYears(1) : null;
        this.accruedInterest = BigDecimal.ZERO;
        this.member = member;
    }

    @PostLoad
    @PrePersist
    void applyDefaultAccountType() {
        if (accountType == null) {
            accountType = AccountType.DEPOSIT_WITHDRAWAL;
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
        if (annualInterestRate == null) {
            annualInterestRate = accountType.getAnnualInterestRate();
        }
        if (accruedInterest == null) {
            accruedInterest = BigDecimal.ZERO;
        }
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
        if (lastInterestCalculatedAt == null) {
            lastInterestCalculatedAt = openedAt;
        }
        if (accountType.isInterestBearing() && maturityAt == null) {
            maturityAt = openedAt.plusYears(1);
        }
    }

    public boolean isInterestBearing() {
        return accountType != null && accountType.isInterestBearing();
    }

    public BigDecimal calculateAccruedInterestAt(LocalDateTime calculationTime) {
        BigDecimal accumulated = accruedInterest == null ? BigDecimal.ZERO : accruedInterest;
        if (!isInterestBearing() || accountStatus == AccountStatus.TERMINATED) {
            return accumulated;
        }

        LocalDateTime start = lastInterestCalculatedAt == null ? openedAt : lastInterestCalculatedAt;
        LocalDateTime end = maturityAt != null && calculationTime.isAfter(maturityAt)
                ? maturityAt
                : calculationTime;
        if (start == null || end == null || !end.isAfter(start) || amount <= 0L) {
            return accumulated;
        }

        long seconds = Duration.between(start, end).getSeconds();
        BigDecimal pending = BigDecimal.valueOf(amount)
                .multiply(annualInterestRate)
                .multiply(BigDecimal.valueOf(seconds))
                .divide(BigDecimal.valueOf(365L * 24L * 60L * 60L), 10, RoundingMode.HALF_UP);

        return accumulated.add(pending);
    }

    public void accrueInterestUntil(LocalDateTime calculationTime) {
        if (!isInterestBearing() || accountStatus == AccountStatus.TERMINATED) {
            return;
        }

        LocalDateTime end = maturityAt != null && calculationTime.isAfter(maturityAt)
                ? maturityAt
                : calculationTime;
        accruedInterest = calculateAccruedInterestAt(calculationTime);
        if (lastInterestCalculatedAt == null || end.isAfter(lastInterestCalculatedAt)) {
            lastInterestCalculatedAt = end;
        }
        if (maturityAt != null && !calculationTime.isBefore(maturityAt)) {
            accountStatus = AccountStatus.MATURED;
        }
    }

    public long terminateAndGetInterest(LocalDateTime terminationTime) {
        accrueInterestUntil(terminationTime);
        long payableInterest = accruedInterest.setScale(0, RoundingMode.DOWN).longValue();
        accountStatus = AccountStatus.TERMINATED;
        interestPaidAt = terminationTime;
        interestPaidAmount = payableInterest;
        return payableInterest;
    }

    public void clearAmount() {
        amount = 0L;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void addAmount(long amount) {
        this.amount += amount;
    }

    public void subAmount(long amount) {
        this.amount -= amount;
    }
}
