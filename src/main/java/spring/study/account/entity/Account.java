package spring.study.account.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.member.entity.Member;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "account")
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 6L;

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
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AccountInterest interestDetail;

    @JsonIgnore
    @OneToOne(mappedBy = "savingsAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SavingsAutoTransfer savingsAutoTransfer;

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
        this.openedAt = LocalDateTime.now();
        this.member = member;
        initializeInterestDetail();
    }

    @PostLoad
    @PrePersist
    void applyDefaults() {
        if (accountType == null) {
            accountType = AccountType.DEPOSIT_WITHDRAWAL;
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
        initializeInterestDetail();
    }

    public boolean isInterestBearing() {
        return accountType != null && accountType.isInterestBearing();
    }

    public BigDecimal calculateAccruedInterestAt(LocalDateTime calculationTime) {
        if (!isInterestBearing()) {
            return BigDecimal.ZERO;
        }
        initializeInterestDetail();
        return interestDetail.calculateAccruedInterestAt(amount, accountStatus, calculationTime);
    }

    public void accrueInterestUntil(LocalDateTime calculationTime) {
        if (!isInterestBearing() || accountStatus == AccountStatus.TERMINATED) {
            return;
        }
        initializeInterestDetail();
        interestDetail.accrueUntil(amount, accountStatus, calculationTime);
        if (!calculationTime.isBefore(interestDetail.getMaturityAt())) {
            accountStatus = AccountStatus.MATURED;
        }
    }

    public long terminateAndGetInterest(LocalDateTime terminationTime) {
        accrueInterestUntil(terminationTime);
        long payableInterest = interestDetail.settle(terminationTime);
        accountStatus = AccountStatus.TERMINATED;
        if (savingsAutoTransfer != null) {
            savingsAutoTransfer.disable();
        }
        return payableInterest;
    }

    public void configureSavingsAutoTransfer(Account sourceAccount, long monthlyAmount, int paymentDay, LocalDate configuredDate) {
        savingsAutoTransfer = SavingsAutoTransfer.create(
                this,
                sourceAccount,
                monthlyAmount,
                paymentDay,
                configuredDate
        );
    }

    public void configureTimeDepositTerm(int maturityMonths) {
        if (accountType != AccountType.TIME_DEPOSIT) {
            throw new IllegalStateException("예금 계좌에만 만기 기간을 설정할 수 있습니다");
        }
        initializeInterestDetail();
        interestDetail.configureTerm(openedAt, maturityMonths);
    }

    public void completeSavingsPayment() {
        if (savingsAutoTransfer != null) {
            savingsAutoTransfer.completePayment(getMaturityAt());
        }
    }

    public void completeInitialSavingsPayment(LocalDate paymentDate) {
        if (savingsAutoTransfer != null) {
            savingsAutoTransfer.completeInitialPayment(paymentDate, getMaturityAt());
        }
    }

    public void recordSavingsFailureNotification(LocalDate notificationDate) {
        if (savingsAutoTransfer != null) {
            savingsAutoTransfer.recordFailureNotification(notificationDate);
        }
    }

    public boolean isSavingsAutoTransferConfigured() {
        return accountType == AccountType.INSTALLMENT_SAVINGS
                && savingsAutoTransfer != null
                && savingsAutoTransfer.isConfigured();
    }

    public BigDecimal getAnnualInterestRate() {
        return interestDetail == null ? BigDecimal.ZERO : interestDetail.getAnnualInterestRate();
    }

    public LocalDateTime getMaturityAt() {
        return interestDetail == null ? null : interestDetail.getMaturityAt();
    }

    public LocalDateTime getLastInterestCalculatedAt() {
        return interestDetail == null ? null : interestDetail.getLastCalculatedAt();
    }

    public BigDecimal getAccruedInterest() {
        return interestDetail == null ? BigDecimal.ZERO : interestDetail.getAccruedInterest();
    }

    public LocalDateTime getInterestPaidAt() {
        return interestDetail == null ? null : interestDetail.getPaidAt();
    }

    public long getInterestPaidAmount() {
        return interestDetail == null ? 0L : interestDetail.getPaidAmount();
    }

    public Integer getMaturityMonths() {
        return interestDetail == null ? null : interestDetail.getTermMonths();
    }

    public Account getSavingsSourceAccount() {
        return savingsAutoTransfer == null ? null : savingsAutoTransfer.getSourceAccount();
    }

    public Long getMonthlySavingsAmount() {
        return savingsAutoTransfer == null ? null : savingsAutoTransfer.getMonthlyAmount();
    }

    public Integer getMonthlySavingsDay() {
        return savingsAutoTransfer == null ? null : savingsAutoTransfer.getPaymentDay();
    }

    public LocalDate getNextSavingsPaymentDate() {
        return savingsAutoTransfer == null ? null : savingsAutoTransfer.getNextPaymentDate();
    }

    public LocalDate getLastSavingsFailureNotificationDate() {
        return savingsAutoTransfer == null ? null : savingsAutoTransfer.getLastFailureNotificationDate();
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        initializeInterestDetail();
        interestDetail.setAnnualInterestRate(annualInterestRate);
    }

    public void setMaturityAt(LocalDateTime maturityAt) {
        initializeInterestDetail();
        interestDetail.setMaturityAt(maturityAt);
    }

    public void setLastInterestCalculatedAt(LocalDateTime calculationTime) {
        initializeInterestDetail();
        interestDetail.setLastCalculatedAt(calculationTime);
    }

    public void setAccruedInterest(BigDecimal accruedInterest) {
        initializeInterestDetail();
        interestDetail.setAccruedInterest(accruedInterest);
    }

    public void clearAmount() {
        amount = 0L;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void addAmount(long amount) {
        this.amount = Math.addExact(this.amount, amount);
    }

    public void subAmount(long amount) {
        this.amount = Math.subtractExact(this.amount, amount);
    }

    private void initializeInterestDetail() {
        if (isInterestBearing() && interestDetail == null && openedAt != null) {
            interestDetail = AccountInterest.create(this, accountType.getAnnualInterestRate(), openedAt);
        }
    }
}
