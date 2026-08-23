package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Objects;

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
    private boolean cancelable;

    public AccountTransactionResponseDto(AccountTransaction entity) {
        this(entity, null);
    }

    public AccountTransactionResponseDto(AccountTransaction entity, Member member) {
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
        this.cancelable = entity.getTransactionStatus() == AccountTransactionStatus.COMPLETED
                && entity.getTransactionType() != AccountTransactionType.CANCEL
                && entity.getTransactionType() != AccountTransactionType.INTEREST
                && entity.getTransactionType() != AccountTransactionType.TERMINATION
                && entity.getTransactionType() != AccountTransactionType.SAVINGS_PAYMENT
                && entity.getTransactionType() != AccountTransactionType.TIME_DEPOSIT_OPENING
                && entity.getTransactionTime().plusDays(1).isAfter(LocalDateTime.now())
                && canMemberCancel(entity, member);
    }

    private boolean canMemberCancel(AccountTransaction entity, Member member) {
        if (member == null || member.getId() == null) {
            return false;
        }

        return switch (entity.getTransactionType()) {
            case DEPOSIT, REFUND -> isAccountOwner(entity.getDepositAccount(), member);
            default -> isAccountOwner(entity.getWithdrawalAccount(), member);
        };
    }

    private boolean isAccountOwner(Account account, Member member) {
        return account != null
                && account.getMember() != null
                && Objects.equals(account.getMember().getId(), member.getId());
    }
}
