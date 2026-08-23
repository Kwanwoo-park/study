package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class AccountResponseDto {
    private String account;
    private long amount;
    private String name;
    private AccountType accountType;
    private String accountTypeName;
    private AccountStatus accountStatus;
    private BigDecimal annualInterestRate;
    private BigDecimal annualInterestRatePercent;
    private LocalDateTime openedAt;
    private LocalDateTime maturityAt;
    private long estimatedInterest;
    private long interestPaidAmount;
    private String savingsSourceAccount;
    private Long monthlySavingsAmount;
    private Integer monthlySavingsDay;
    private LocalDate nextSavingsPaymentDate;
    private Integer maturityMonths;
    private boolean outgoingTransferAllowed;
    private boolean depositAllowed;

    public AccountResponseDto(Account entity) {
        this.account = entity.getAccount();
        this.amount = entity.getAmount();
        this.name = entity.getName();
        this.accountType = entity.getAccountType();
        this.accountTypeName = entity.getAccountType().getDisplayName();
        this.accountStatus = entity.getAccountStatus();
        this.annualInterestRate = entity.getAnnualInterestRate();
        this.annualInterestRatePercent = entity.getAnnualInterestRate().multiply(BigDecimal.valueOf(100L));
        this.openedAt = entity.getOpenedAt();
        this.maturityAt = entity.getMaturityAt();
        this.estimatedInterest = entity.getAccountStatus() == AccountStatus.TERMINATED
                ? entity.getInterestPaidAmount()
                : entity.calculateAccruedInterestAt(LocalDateTime.now()).setScale(0, RoundingMode.DOWN).longValue();
        this.interestPaidAmount = entity.getInterestPaidAmount();
        this.savingsSourceAccount = entity.getSavingsSourceAccount() == null
                ? null
                : entity.getSavingsSourceAccount().getAccount();
        this.monthlySavingsAmount = entity.getMonthlySavingsAmount();
        this.monthlySavingsDay = entity.getMonthlySavingsDay();
        this.nextSavingsPaymentDate = entity.getNextSavingsPaymentDate();
        this.maturityMonths = entity.getMaturityMonths();
        this.outgoingTransferAllowed = entity.getAccountType() == AccountType.DEPOSIT_WITHDRAWAL;
        this.depositAllowed = entity.getAccountType() != AccountType.TIME_DEPOSIT;
    }

    @Override
    public String toString() {
        return "AccountResponseDto{" +
                "account='" + account + '\'' +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                '}';
    }
}
