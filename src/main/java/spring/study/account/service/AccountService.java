package spring.study.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.repository.AccountRepository;
import spring.study.account.repository.AccountTransactionRepository;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final NotificationService notificationService;

    @Transactional
    public Account createAccount(Member member) {
        String accountFirst = "919";
        String accountLast = createAccountLast();
        String accountNumber = accountFirst + accountLast;

        while (accountRepository.existsById(accountNumber)) {
            accountLast = createAccountLast();
            accountNumber = accountFirst + accountLast;
        }

        Account account = Account.builder()
                .account(accountNumber)
                .amount(0L)
                .name("Kwanwoo site account")
                .member(member)
                .build();

        return accountRepository.save(account);
    }

    public Account findByAccount(String accountNum) {
        return accountRepository.findById(accountNum).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 계좌입니다"
        ));
    }

    public List<Account> findByMember(Member member) {
        return accountRepository.findByMember(member);
    }

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public boolean existsByAccount(String accountNum) {
        return !accountRepository.existsById(accountNum);
    }

    @Transactional
    public void changeAccountName(String accountNum, String name) {
        Account account = findByAccount(accountNum);

        account.changeName(name);
    }

    @Transactional
    public Account tranAccount(AccountTranDto dto) {
        if (dto.getAmount() < 10000) {
            throw new IllegalArgumentException("이체 금액은 1만원 이상이어야 합니다");
        }

        if (Objects.equals(dto.getAccount(), dto.getTranAccount())) {
            throw new IllegalArgumentException("같은 계좌로 이체할 수 없습니다");
        }

        Account account = findByAccount(dto.getAccount());
        Account tranAccount = findByAccount(dto.getTranAccount());

        if (account.getAmount() - dto.getAmount() < 0) {
            throw new IllegalArgumentException("이체 금액이 계좌 잔액보다 큽니다");
        }

        account.subAmount(dto.getAmount());
        tranAccount.addAmount(dto.getAmount());
        AccountTransaction transaction = accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(AccountTransactionType.TRANSFER)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(dto.getAmount())
                .fee(0L)
                .withdrawalAccount(account)
                .depositAccount(tranAccount)
                .balanceAfterTransaction(account.getAmount())
                .counterpartyName(tranAccount.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(LocalDateTime.now())
                .build());
        notifyTransaction(transaction);

        return account;
    }

    @Transactional
    public Account deposit(String accountNum, Long amount) {
        if (amount == null || amount < 10000) {
            throw new IllegalArgumentException("입금 금액은 1만원 이상이어야 합니다");
        }

        Account account = findByAccount(accountNum);
        account.addAmount(amount);
        AccountTransaction transaction = accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(AccountTransactionType.DEPOSIT)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount)
                .fee(0L)
                .depositAccount(account)
                .balanceAfterTransaction(account.getAmount())
                .counterpartyName(account.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(LocalDateTime.now())
                .build());
        notifyTransaction(transaction);

        return account;
    }

    @Transactional
    public void subAmount(String accountNum, Long amount) {
        Account account = findByAccount(accountNum);

        account.subAmount(amount);
    }

    @Transactional
    public void deleteByAccount(String accountNum) {
        accountRepository.deleteById(accountNum);
    }

    @Transactional
    public void deleteByMember(Member member) {
        accountRepository.deleteByMember(member);
    }

    private String createAccountLast() {
        long timestamp = Instant.now().toEpochMilli();
        int digitLength = ThreadLocalRandom.current().nextInt(10, 14);
        long min = (long) Math.pow(10, digitLength - 1);
        long max = (long) Math.pow(10, digitLength);
        long range = max - min;
        long randomValue = ThreadLocalRandom.current().nextLong(range);

        return String.valueOf(min + Math.floorMod(timestamp + randomValue, range));
    }

    void notifyTransaction(AccountTransaction transaction) {
        if (transaction.getWithdrawalAccount() != null) {
            notifyAccountMember(
                    transaction.getWithdrawalAccount(),
                    createTransactionMessage(transaction, transaction.getWithdrawalAccount())
            );
        }

        if (transaction.getDepositAccount() != null) {
            notifyAccountMember(
                    transaction.getDepositAccount(),
                    createTransactionMessage(transaction, transaction.getDepositAccount())
            );
        }
    }

    private void notifyAccountMember(Account account, String message) {
        Member member = account.getMember();
        if (member == null) {
            return;
        }

        notificationService.createNotification(member, message, Group.TRAN, account.getAccount());
    }

    private String createTransactionMessage(AccountTransaction transaction, Account account) {
        String amount = NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.getAmount());
        String status = transaction.getTransactionStatus() == AccountTransactionStatus.CANCELED ? "취소" : "완료";
        String transactionType = switch (transaction.getTransactionType()) {
            case TRANSFER -> "이체";
            case DEPOSIT -> "입금";
            case WITHDRAWAL -> "출금";
            case PAYMENT -> "결제";
            case REFUND -> "환불";
            case FEE -> "수수료";
            case CANCEL -> "취소";
        };

        return account.getName() + " 계좌에서 " + amount + "원 " + transactionType + " 거래가 " + status + "되었습니다.";
    }
}
