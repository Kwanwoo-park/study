package spring.study.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "account_transaction")
public class AccountTransaction implements Serializable {
    @Serial
    private static final long serialVersionUID = 6L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountTransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountTransactionStatus transactionStatus;

    @NotNull
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_account")
    private Account withdrawalAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_account")
    private Account depositAccount;

    @Column(nullable = false)
    private long balanceAfterTransaction;

    @Column(length = 200)
    private String memo;

    @Column(length = 100)
    private String counterpartyName;

    @Column(length = 100)
    private String bankName;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Builder
    public AccountTransaction(
            Long id,
            AccountTransactionType transactionType,
            AccountTransactionStatus transactionStatus,
            long amount,
            long fee,
            Account withdrawalAccount,
            Account depositAccount,
            long balanceAfterTransaction,
            String memo,
            String counterpartyName,
            String bankName,
            LocalDateTime transactionTime
    ) {
        this.id = id;
        this.transactionType = transactionType;
        this.transactionStatus = transactionStatus;
        this.amount = amount;
        this.fee = fee;
        this.withdrawalAccount = withdrawalAccount;
        this.depositAccount = depositAccount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.memo = memo;
        this.counterpartyName = counterpartyName;
        this.bankName = bankName;
        this.transactionTime = transactionTime;
    }
}
