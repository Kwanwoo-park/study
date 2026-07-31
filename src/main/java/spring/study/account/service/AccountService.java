package spring.study.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountStatus;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.entity.AccountTransactionStatus;
import spring.study.account.entity.AccountTransactionType;
import spring.study.account.entity.AccountType;
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
        return createAccount(member, AccountType.DEPOSIT_WITHDRAWAL);
    }

    @Transactional
    public Account createAccount(Member member, AccountType accountType) {
        AccountType resolvedType = accountType == null
                ? AccountType.DEPOSIT_WITHDRAWAL
                : accountType;
        if (resolvedType.isInterestBearing()
                && !accountRepository.existsByMemberAndAccountTypeAndAccountStatus(
                member,
                AccountType.DEPOSIT_WITHDRAWAL,
                AccountStatus.ACTIVE
        )) {
            throw new IllegalArgumentException("예적금 계좌를 만들려면 먼저 활성 상태의 입출금 계좌가 필요합니다");
        }

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
                .name(createDefaultAccountName(resolvedType))
                .accountType(resolvedType)
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

    public List<Account> findActiveByMember(Member member) {
        return accountRepository.findByMemberAndAccountStatus(member, AccountStatus.ACTIVE);
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

        LocalDateTime transactionTime = LocalDateTime.now();
        prepareBalanceChange(account, transactionTime);
        prepareBalanceChange(tranAccount, transactionTime);

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
                .transactionTime(transactionTime)
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
        LocalDateTime transactionTime = LocalDateTime.now();
        prepareBalanceChange(account, transactionTime);
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
                .transactionTime(transactionTime)
                .build());
        notifyTransaction(transaction);

        return account;
    }

    @Transactional
    public void subAmount(String accountNum, Long amount) {
        Account account = findByAccount(accountNum);

        prepareBalanceChange(account, LocalDateTime.now());
        account.subAmount(amount);
    }

    @Transactional
    public AccountSettlementResult terminateInterestAccount(String accountNumber,
                                                            String settlementAccountNumber,
                                                            Member member) {
        if (settlementAccountNumber == null || settlementAccountNumber.isBlank()) {
            throw new IllegalArgumentException("정산받을 입출금 계좌를 선택해주세요");
        }

        Account source = findForUpdate(accountNumber);
        Account settlementAccount = findForUpdate(settlementAccountNumber);
        validateSettlement(source, settlementAccount, member);

        LocalDateTime terminationTime = LocalDateTime.now();
        boolean matured = source.getMaturityAt() != null && !terminationTime.isBefore(source.getMaturityAt());
        long principal = source.getAmount();
        long interest = source.terminateAndGetInterest(terminationTime);
        long settlementAmount = Math.addExact(principal, interest);

        source.clearAmount();
        settlementAccount.addAmount(principal);
        AccountTransaction principalTransaction = accountTransactionRepository.save(AccountTransaction.builder()
                .transactionType(AccountTransactionType.TERMINATION)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(principal)
                .fee(0L)
                .withdrawalAccount(source)
                .depositAccount(settlementAccount)
                .balanceAfterTransaction(settlementAccount.getAmount())
                .memo(matured ? "만기 원금 정산" : "중도 해지 원금 정산")
                .counterpartyName(source.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(terminationTime)
                .build());

        AccountTransaction interestTransaction = null;
        if (interest > 0L) {
            settlementAccount.addAmount(interest);
            interestTransaction = accountTransactionRepository.save(AccountTransaction.builder()
                    .transactionType(AccountTransactionType.INTEREST)
                    .transactionStatus(AccountTransactionStatus.COMPLETED)
                    .amount(interest)
                    .fee(0L)
                    .depositAccount(settlementAccount)
                    .balanceAfterTransaction(settlementAccount.getAmount())
                    .memo(matured ? "만기 이자" : "중도 해지 이자")
                    .counterpartyName(source.getName())
                    .bankName("Kwanwoo site account")
                    .transactionTime(terminationTime)
                    .build());
        }

        notifyTransaction(principalTransaction);
        if (interestTransaction != null) {
            notifyTransaction(interestTransaction);
        }

        return new AccountSettlementResult(
                source.getAccount(),
                settlementAccount.getAccount(),
                principal,
                interest,
                settlementAmount,
                matured
        );
    }

    @Transactional
    public void markMaturedAccounts(LocalDateTime now) {
        accountRepository.findByAccountStatusAndMaturityAtLessThanEqual(AccountStatus.ACTIVE, now)
                .forEach(account -> account.accrueInterestUntil(now));
    }

    public void accrueInterest(Account account, LocalDateTime calculationTime) {
        if (account != null) {
            prepareBalanceChange(account, calculationTime);
        }
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

    private String createDefaultAccountName(AccountType accountType) {
        AccountType resolvedType = accountType == null
                ? AccountType.DEPOSIT_WITHDRAWAL
                : accountType;

        return resolvedType.getDefaultAccountName();
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
            case INTEREST -> "이자";
            case TERMINATION -> "해지 정산";
        };

        return account.getName() + " 계좌에서 " + amount + "원 " + transactionType + " 거래가 " + status + "되었습니다.";
    }

    private Account findForUpdate(String accountNumber) {
        return accountRepository.findByAccountForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌입니다"));
    }

    private void validateActive(Account account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("만기 또는 해지된 계좌는 거래할 수 없습니다");
        }
    }

    private void prepareBalanceChange(Account account, LocalDateTime calculationTime) {
        account.accrueInterestUntil(calculationTime);
        validateActive(account);
    }

    private void validateSettlement(Account source, Account settlementAccount, Member member) {
        if (source.getMember() == null || !source.getMember().getId().equals(member.getId())
                || settlementAccount.getMember() == null
                || !settlementAccount.getMember().getId().equals(member.getId())) {
            throw new SecurityException("본인 계좌로만 해지 정산할 수 있습니다");
        }
        if (!source.isInterestBearing()) {
            throw new IllegalArgumentException("예적금 계좌만 해지할 수 있습니다");
        }
        if (source.getAccountStatus() == AccountStatus.TERMINATED) {
            throw new IllegalArgumentException("이미 해지된 계좌입니다");
        }
        if (settlementAccount.getAccountType() != AccountType.DEPOSIT_WITHDRAWAL
                || settlementAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("활성 상태의 입출금 계좌로만 정산할 수 있습니다");
        }
        if (source.getAccount().equals(settlementAccount.getAccount())) {
            throw new IllegalArgumentException("해지할 계좌와 정산 계좌는 다르게 선택해주세요");
        }
    }
}
