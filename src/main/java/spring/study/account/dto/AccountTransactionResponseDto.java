package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AccountTransactionResponseDto {
    private Long id;
    private AccountTransactionType transactionType;
    private AccountTransactionStatus transactionStatus;
    private long amount;
    private long fee;
    private String withdrawalAccount;
    private String depositAccount;
    private long balanceAfterTransaction;
    private String memo;
    private String counterpartyName;
    private String bankName;
    private LocalDateTime transactionTime;

    public AccountTransactionResponseDto(AccountTransaction entity) {
        this.id = entity.getId();
        this.transactionType = entity.getTransactionType();
        this.transactionStatus = entity.getTransactionStatus();
        this.amount = entity.getAmount();
        this.fee = entity.getFee();
        this.withdrawalAccount = entity.getWithdrawalAccount() == null ? null : entity.getWithdrawalAccount().getAccount();
        this.depositAccount = entity.getDepositAccount() == null ? null : entity.getDepositAccount().getAccount();
        this.balanceAfterTransaction = entity.getBalanceAfterTransaction();
        this.memo = entity.getMemo();
        this.counterpartyName = entity.getCounterpartyName();
        this.bankName = entity.getBankName();
        this.transactionTime = entity.getTransactionTime();
    }
}
