package spring.study.account.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountTransactionResponseDto;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccountOrDepositAccount(account, account, createPageRequest(page));
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByWithdrawalAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccount(account, createPageRequest(page));
    }

    @Transactional(readOnly = true)
    public Page<AccountTransaction> findByDepositAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByDepositAccount(account, createPageRequest(page));
    }

    @Transactional
    public Cancellation cancelTransaction(Long transactionId, Member member) {
        AccountTransaction transaction = accountTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래입니다"));

        validateCancelable(transaction, member);

        Account originalDepositAccount = transaction.getDepositAccount();
        Account originalWithdrawalAccount = transaction.getWithdrawalAccount();
        Account[] lockedAccounts = lockAccounts(originalDepositAccount, originalWithdrawalAccount);
        Account reversalWithdrawalAccount = accountByNumber(lockedAccounts, originalDepositAccount);
        Account reversalDepositAccount = accountByNumber(lockedAccounts, originalWithdrawalAccount);
        LocalDateTime cancellationTime = LocalDateTime.now();

        if (reversalWithdrawalAccount != null) {
            accountService.validateOutgoingTransaction(reversalWithdrawalAccount);
            accountService.accrueInterest(reversalWithdrawalAccount, cancellationTime);

            if (reversalWithdrawalAccount.getAmount() < transaction.getAmount()) {
                throw new IllegalArgumentException("취소할 계좌의 잔액이 부족합니다");
            }

            reversalWithdrawalAccount.subAmount(transaction.getAmount());
        }

        if (reversalDepositAccount != null) {
            accountService.accrueInterest(reversalDepositAccount, cancellationTime);
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

    private AccountTransaction record(AccountTransactionType type, Account source, Account target, long amount,
                                      long balance, String memo, LocalDateTime time) {
        return accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(type).transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount).fee(0L).withdrawalAccount(source).depositAccount(target)
                .balanceAfterTransaction(balance).memo(memo).counterpartyName(target.getName())
                .bankName("Kwanwoo site account").transactionTime(time).build());
    }

    private void validateCancelable(AccountTransaction transaction, Member member) {
        if (!isTransactionOwner(transaction, member)) {
            String message = transaction.getTransactionType() == AccountTransactionType.TRANSFER ? "계좌이체는 보낸 사람만 취소할 수 있습니다" : "본인 거래만 취소할 수 있습니다";
            throw new SecurityException(message);
        } else if (transaction.getTransactionType() == AccountTransactionType.CANCEL) {
            throw new IllegalArgumentException("취소 거래는 다시 취소할 수 없습니다");
        } else if (transaction.getTransactionType() == AccountTransactionType.INTEREST || transaction.getTransactionType() == AccountTransactionType.TERMINATION || transaction.getTransactionType() == AccountTransactionType.SAVINGS_PAYMENT || transaction.getTransactionType() == AccountTransactionType.TIME_DEPOSIT_OPENING) {
            throw new IllegalArgumentException("이자, 해지 정산 및 예적금 개설 거래는 취소할 수 없습니다");
        } else if (transaction.getTransactionStatus() != AccountTransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("완료된 거래만 취소할 수 있습니다");
        } else if (transaction.getTransactionTime().plusDays(1).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("거래 후 하루가 지난 거래는 취소할 수 없습니다");
        }
    }

    private boolean isTransactionOwner(AccountTransaction transaction, Member member) {
        Long memberId = member.getId();
        Account withdrawalAccount = transaction.getWithdrawalAccount();
        Account depositAccount = transaction.getDepositAccount();

        return switch (transaction.getTransactionType()) {
            case DEPOSIT, REFUND -> isAccountOwner(depositAccount, memberId);
            default -> isAccountOwner(withdrawalAccount, memberId);
        };
    }

    private Account[] lockAccounts(Account first, Account second) {
        if (first == null && second == null) return new Account[0];
        else if (first == null) return new Account[]{accountService.findByAccountForUpdate(second.getAccount())};
        else if (second == null) return new Account[]{accountService.findByAccountForUpdate(first.getAccount())};

        String firstNumber = first.getAccount().compareTo(second.getAccount()) <= 0 ? first.getAccount() : second.getAccount();
        String secondNumber = firstNumber.equals(first.getAccount()) ? second.getAccount() : first.getAccount();

        return new Account[]{ accountService.findByAccountForUpdate(firstNumber), accountService.findByAccountForUpdate(secondNumber) };
    }

    private Account accountByNumber(Account[] accounts, Account original) {
        if (original == null) return null;

        for (Account account : accounts) {
            if (account.getAccount().equals(original.getAccount())) return account;
        }

        throw new IllegalStateException("거래 계좌 잠금에 실패했습니다");
    }

    private boolean isAccountOwner(Account account, Long memberId) {
        return account != null && account.getMember() != null && account.getMember().getId().equals(memberId);
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
