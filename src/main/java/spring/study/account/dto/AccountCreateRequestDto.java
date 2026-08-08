package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.account.entity.AccountType;

@Getter
@Setter
@NoArgsConstructor
public class AccountCreateRequestDto {
    private AccountType accountType;
    private String savingsSourceAccount;
    private Long monthlySavingsAmount;
    private Integer monthlySavingsDay;
    private Boolean autoTerminationAcknowledged;
    private String timeDepositSourceAccount;
    private Long timeDepositAmount;
    private Integer maturityMonths;
}
