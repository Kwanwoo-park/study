package spring.study.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountTransactionService {
    private static final int PAGE_SIZE = 20;

    private final AccountService accountService;
    private final AccountTransactionRepository accountTransactionRepository;

    @Transactional
    public AccountTransaction save(AccountTransaction accountTransaction) {
        return accountTransactionRepository.save(accountTransaction);
    }

    public Page<AccountTransaction> findByAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccountOrDepositAccount(
                account,
                account,
                createPageRequest(page)
        );
    }

    public Page<AccountTransaction> findByWithdrawalAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccount(account, createPageRequest(page));
    }

    public Page<AccountTransaction> findByDepositAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByDepositAccount(account, createPageRequest(page));
    }

    @Transactional
    public AccountTransaction cancelTransaction(Long transactionId, Member member) {
        AccountTransaction transaction = accountTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래입니다"));

        validateCancelable(transaction, member);

        Account reversalWithdrawalAccount = transaction.getDepositAccount();
        Account reversalDepositAccount = transaction.getWithdrawalAccount();

        if (reversalWithdrawalAccount != null) {
            if (reversalWithdrawalAccount.getAmount() < transaction.getAmount()) {
                throw new IllegalArgumentException("취소할 계좌의 잔액이 부족합니다");
            }
            reversalWithdrawalAccount.subAmount(transaction.getAmount());
        }

        if (reversalDepositAccount != null) {
            reversalDepositAccount.addAmount(transaction.getAmount());
        }

        transaction.cancel();

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
                .transactionTime(LocalDateTime.now())
                .build());

        accountService.notifyTransaction(transaction);
        return cancelTransaction;
    }

    private void validateCancelable(AccountTransaction transaction, Member member) {
        if (!isTransactionOwner(transaction, member)) {
            throw new SecurityException("본인 거래만 취소할 수 있습니다");
        }

        if (transaction.getTransactionType() == AccountTransactionType.CANCEL) {
            throw new IllegalArgumentException("취소 거래는 다시 취소할 수 없습니다");
        }

        if (transaction.getTransactionStatus() != AccountTransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 거래만 취소할 수 있습니다");
        }

        if (transaction.getTransactionTime().plusDays(1).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("거래 후 하루가 지난 거래는 취소할 수 없습니다");
        }
    }

    private boolean isTransactionOwner(AccountTransaction transaction, Member member) {
        Long memberId = member.getId();
        Account withdrawalAccount = transaction.getWithdrawalAccount();
        Account depositAccount = transaction.getDepositAccount();

        return isAccountOwner(withdrawalAccount, memberId) || isAccountOwner(depositAccount, memberId);
    }

    private boolean isAccountOwner(Account account, Long memberId) {
        return account != null
                && account.getMember() != null
                && account.getMember().getId().equals(memberId);
    }

    private long getBalanceAfterCancel(Account withdrawalAccount, Account depositAccount) {
        if (withdrawalAccount != null) {
            return withdrawalAccount.getAmount();
        }

        if (depositAccount != null) {
            return depositAccount.getAmount();
        }

        return 0L;
    }

    private PageRequest createPageRequest(int page) {
        return PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Order.desc("transactionTime"), Sort.Order.desc("id"))
        );
    }
}
