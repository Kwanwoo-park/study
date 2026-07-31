package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

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
