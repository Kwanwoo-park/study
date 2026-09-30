package spring.study.account.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountTransactionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountTransactionService {
    private static final int PAGE_SIZE = 20;

    private final AccountTransactionRepository accountTransactionRepository;

    @Transactional
    public AccountTransaction save(AccountTransaction accountTransaction) {
        return accountTransactionRepository.save(accountTransaction);
    }

    @Transactional(readOnly = true)
    public AccountTransaction findUpdate(Long transactionId) {
        return accountTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래입니다"));
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByAccount(Account account, int page) {
        return accountTransactionRepository.findByWithdrawalAccountOrDepositAccount(account, account, createPageRequest(page));
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByWithdrawalAccount(Account account, int page) {
        return accountTransactionRepository.findByWithdrawalAccount(account, createPageRequest(page));
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByDepositAccount(Account account, int page) {
        return accountTransactionRepository.findByDepositAccount(account, createPageRequest(page));
    }

    @Transactional
    public Cancellation cancelTransaction(AccountTransaction transaction, Account reversalWithdrawalAccount, Account reversalDepositAccount, LocalDateTime cancellationTime) {
        AccountTransaction cancelTransaction = accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(AccountTransactionType.CANCEL)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(transaction.getAmount())
                .fee(0L)
                .withdrawalAccount(reversalWithdrawalAccount)
                .depositAccount(reversalDepositAccount)
                .balanceAfterTransaction(getBalanceAfterCancel(reversalWithdrawalAccount, reversalDepositAccount))
                .memo("원 거래 번호 " + transaction.getId() + " 취소")
                .counterpartyName(transaction.getCounterpartyName())
                .bankName(transaction.getBankName())
                .transactionTime(cancellationTime)
                .build());

        return new Cancellation(transaction, cancelTransaction);
    }

    public record Cancellation(AccountTransaction original, AccountTransaction cancellation) {}

    @Transactional
    public AccountTransaction recordOpening(Account source, Account account, long amount) {
        boolean deposit = account.getAccountType() == spring.study.account.entity.AccountType.TIME_DEPOSIT;
        return record(deposit ? AccountTransactionType.TIME_DEPOSIT_OPENING : AccountTransactionType.SAVINGS_PAYMENT,
                source, account, amount, source.getAmount(),
                deposit ? account.getMaturityMonths() + "개월 예금 개설" : "적금 개설 첫 납입", LocalDateTime.now());
    }

    @Transactional
    public AccountTransaction recordTransfer(Account source, Account target, long amount, LocalDateTime time) {
        return record(AccountTransactionType.TRANSFER, source, target, amount, source.getAmount(), null, time);
    }

    @Transactional
    public AccountTransaction recordDeposit(Account target, long amount, LocalDateTime time) {
        return record(AccountTransactionType.DEPOSIT, null, target, amount, target.getAmount(), null, time);
    }

    @Transactional
    public List<AccountTransaction> recordSettlement(Account source, Account target, AccountSettlementResult result, LocalDateTime time) {
        List<AccountTransaction> records = new ArrayList<>();
        String memo = result.matured() ? "만기 해지" : "중도 해지";
        records.add(record(AccountTransactionType.TERMINATION, source, target, result.principal(),
                target.getAmount() - result.interest(), memo + " 정산", time));
        if (result.interest() > 0L) records.add(record(AccountTransactionType.INTEREST, null, target, result.interest(),
                target.getAmount(), memo + " 이자", time));
        return records;
    }

    @Transactional
    public AccountTransaction recordAutoSaving(AccountTransactionType type, Account savings, Account source, long paymentAmount, LocalDateTime processingTime) {
        String memo = "적금 정기 자동이체";

        return record(type, source, savings, paymentAmount, source.getAmount(), memo, processingTime);
    }

    private AccountTransaction record(AccountTransactionType type, Account source, Account target, long amount, long balance, String memo, LocalDateTime time) {
        return accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(type)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount)
                .fee(0L)
                .withdrawalAccount(source)
                .depositAccount(target)
                .balanceAfterTransaction(balance)
                .memo(memo)
                .counterpartyName(target.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(time)
                .build());
    }

    private long getBalanceAfterCancel(Account withdrawalAccount, Account depositAccount) {
        if (withdrawalAccount != null) {
            return withdrawalAccount.getAmount();
        } else if (depositAccount != null) {
            return depositAccount.getAmount();
        }

        return 0L;
    }

    private PageRequest createPageRequest(int page) {
        return PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Order.desc("transactionTime"), Sort.Order.desc("id")));
    }
}
