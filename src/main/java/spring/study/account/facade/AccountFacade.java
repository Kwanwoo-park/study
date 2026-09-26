package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.account.dto.AccountRequestDto;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.entity.*;
import spring.study.account.service.AccountService;
import spring.study.account.service.AccountTransactionService;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountFacade {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;
    private final NotificationService notificationService;

    public ResponseEntity<?> create(Member member, AccountType accountType) {
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setAccountType(accountType);

        return create(member, requestDto);
    }

    public ResponseEntity<?> create(Member member, AccountCreateRequestDto requestDto) {
        Account account, source;

        if (requestDto == null) {
            requestDto = new AccountCreateRequestDto();
            requestDto.setAccountType(AccountType.DEPOSIT_WITHDRAWAL);
        }

        try {
            source = requestDto.getAccountType() == AccountType.INSTALLMENT_SAVINGS ? accountService.resolveSavingsSourceAccount(member, requestDto) : accountService.resolveTimeDepositSourceAccount(member, requestDto);

            account = accountService.createAccount(member, requestDto, source);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }

        if (account.getAccountType() == AccountType.INSTALLMENT_SAVINGS)
            createOpenTransaction(account.getAccountType(), source, account, requestDto.getMonthlySavingsAmount());
        else if (account.getAccountType() == AccountType.TIME_DEPOSIT)
            createOpenTransaction(account.getAccountType(), source, account, requestDto.getTimeDepositAmount());

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "accountNum", account.getAccount(),
                "accountType", account.getAccountType()
        ));
    }

    public ResponseEntity<?> getList(Member member) {
        return ResponseEntity.ok(Map.of(
                "list", accountService.findByMember(member),
                "name", member.getName(),
                "result", 10L
        ));
    }

    @Transactional
    public ResponseEntity<?> tranAccount(AccountTranDto dto, Member member) {
        ResponseEntity<?> validation = validateOwner(dto.getTranAccount());
        if (validation != null) return validation;

        validation = validateOwner(dto.getAccount(), member);
        if (validation != null) return validation;

        validation = checkTran(dto);
        if (validation != null) return validation;

        String firstAccountNumber = dto.getAccount().compareTo(dto.getTranAccount()) <= 0 ? dto.getAccount() : dto.getTranAccount();
        String secondAccountNumber = firstAccountNumber.equals(dto.getAccount()) ? dto.getTranAccount() : dto.getAccount();

        Account firstLockedAccount = accountService.findByAccountForUpdate(firstAccountNumber);
        Account secondLockedAccount = accountService.findByAccountForUpdate(secondAccountNumber);

        Account account = dto.getAccount().equals(firstAccountNumber) ? firstLockedAccount : secondLockedAccount;
        Account tranAccount = dto.getTranAccount().equals(firstAccountNumber) ? firstLockedAccount : secondLockedAccount;

        LocalDateTime time = LocalDateTime.now();

        try {
            accountService.tranAccount(account, tranAccount, dto.getAmount(), time);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }

        createTransaction(AccountTransactionType.TRANSFER, account, tranAccount, dto.getAmount(), time);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "message", "정상적으로 이체되었습니다"
        ));
    }

    public ResponseEntity<?> deposit(AccountTranDto dto, Member member) {
        ResponseEntity<?> validation = validateOwner(dto.getAccount(), member);
        if (validation != null) return validation;

        validation = checkTran(dto);
        if (validation != null) return validation;

        Account account = accountService.findByAccount(dto.getAccount());

        LocalDateTime time = LocalDateTime.now();

        accountService.deposit(account, dto.getAmount(), time);

        createTransaction(AccountTransactionType.DEPOSIT, account, null, dto.getAmount(), time);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "amount", account.getAmount(),
                "message", "정상적으로 입금되었습니다"
        ));
    }

    public ResponseEntity<?> changeAccountName(AccountRequestDto dto, Member member) {
        ResponseEntity<?> validation = validateOwner(dto.getAccount(), member);
        if (validation != null) return validation;

        accountService.changeAccountName(dto.getAccount(), dto.getName());

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> delete(String account, Member member) {
        ResponseEntity<?> validation = validateOwner(account, member);
        if (validation != null) return validation;

        Account target = accountService.findByAccount(account);

        if (target.getAccountType() == AccountType.DEPOSIT_WITHDRAWAL && accountService.hasActiveSavingsUsingSource(target)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "적금 자동이체에 사용 중인 입출금 계좌는 삭제할 수 없습니다"
            ));
        } else if (target.isInterestBearing() && target.getAccountStatus() != AccountStatus.TERMINATED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "예적금 계좌는 해지 정산 기능을 이용해주세요"
            ));
        }

        accountService.deleteByAccount(account);

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> terminate(String accountNumber, String settlementAccountNumber, Member member) {
        ResponseEntity<?> validation = checkAccount(settlementAccountNumber);
        if (validation != null) return validation;

        Account source = accountService.findByAccountForUpdate(accountNumber);
        Account settlementAccount = accountService.findByAccountForUpdate(settlementAccountNumber);

        LocalDateTime time = LocalDateTime.now();

        boolean matured = source.getMaturityAt() != null && !time.isBefore(source.getMaturityAt());
        long principal = source.getAmount();
        long interest = source.terminateAndGetInterest(time);

        try {
            AccountSettlementResult settlement = accountService.terminateInterestAccount(source, settlementAccount, member, matured, principal, interest);

            createTransaction(AccountTransactionType.TERMINATION, source, settlementAccount, principal, time, matured);

            if (interest > 0L)
                createTransaction(AccountTransactionType.INTEREST, source, settlementAccount, interest, time, matured);

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "settlement", settlement,
                    "message", settlement.matured() ? "만기 해지가 완료되었습니다" : "중도 해지가 완료되었습니다"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException | ArithmeticException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }
    }

    private ResponseEntity<?> validateOwner(String accountNumber, Member member) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return accountNotFound();
        }

        if (accountService.existsByAccount(accountNumber)) {
            return accountNotFound();
        }

        Account account = accountService.findByAccount(accountNumber);

        if (!account.getMember().getId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", "본인 계좌만 사용할 수 있습니다"
            ));
        }

        return null;
    }

    private ResponseEntity<?> validateOwner(String tranAccountNumber) {
        if (tranAccountNumber == null || tranAccountNumber.isBlank()) {
            return accountNotFound();
        }

        if (accountService.existsByAccount(tranAccountNumber)) {
            return accountNotFound();
        }

        return null;
    }

    private ResponseEntity<?> checkAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "정산받을 입출금 계좌를 선택해주세요"
            ));

        return null;
    }

    private ResponseEntity<?> accountNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "result", -10L,
                "message", "존재하지 않는 계좌입니다"
        ));
    }

    private ResponseEntity<?> checkTran(AccountTranDto dto) {
        String message = "";

        if (dto.getAmount() < 10000)
            message = "거래 금액은 1만원 이상이어야 합니다";
        else if (dto.getAccount().equals(dto.getTranAccount()))
            message = "같은 계좌로 이체할 수 없습니다";

        if (!message.isBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", message
            ));
        else return null;
    }

    private void createTransaction(AccountTransactionType type, Account account, Account tranAccount, Long amount, LocalDateTime time) {
        AccountTransaction transaction = accountTransactionService.save(AccountTransaction.builder()
                .transactionType(type)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount)
                .fee(0L)
                .withdrawalAccount(type == AccountTransactionType.DEPOSIT ? null : account)
                .depositAccount(type == AccountTransactionType.DEPOSIT ? account : tranAccount)
                .balanceAfterTransaction(account.getAmount())
                .counterpartyName(type == AccountTransactionType.DEPOSIT ? account.getName() : tranAccount.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(time)
                .build());

        notifyTransaction(transaction);
    }

    private void createTransaction(AccountTransactionType type, Account source, Account settlementAccount, Long amount, LocalDateTime time, boolean matured) {
        AccountTransaction transaction = accountTransactionService.save(AccountTransaction.builder()
                .transactionType(type)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount)
                .fee(0L)
                .withdrawalAccount(type == AccountTransactionType.INTEREST ? null : source)
                .depositAccount(settlementAccount)
                .balanceAfterTransaction(settlementAccount.getAmount())
                .memo(matured ? "만기 이자" : "중도 해지 이자")
                .counterpartyName(source.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(time)
                .build());

        notifyTransaction(transaction);
    }

    private void createOpenTransaction(AccountType type, Account source, Account account, long amount) {
        LocalDateTime openingTime = LocalDateTime.now();

        AccountTransaction transaction = accountTransactionService.save(AccountTransaction.builder()
                .transactionType(type == AccountType.TIME_DEPOSIT ? AccountTransactionType.TIME_DEPOSIT_OPENING : AccountTransactionType.SAVINGS_PAYMENT)
                .transactionStatus(AccountTransactionStatus.COMPLETED)
                .amount(amount)
                .fee(0L)
                .withdrawalAccount(source)
                .depositAccount(account)
                .balanceAfterTransaction(source.getAmount())
                .memo(type == AccountType.TIME_DEPOSIT ? account.getMaturityMonths() + "개월 예금 개설" : "적금 개설 첫 납입")
                .counterpartyName(account.getName())
                .bankName("Kwanwoo site account")
                .transactionTime(openingTime)
                .build());

        notifyTransaction(transaction);
    }

    private void notifyTransaction(AccountTransaction transaction) {
        if (transaction.getWithdrawalAccount() != null) {
            notifyAccountMember(transaction.getWithdrawalAccount(), createTransactionMessage(transaction, transaction.getWithdrawalAccount()));
        }

        if (transaction.getDepositAccount() != null) {
            notifyAccountMember(transaction.getDepositAccount(), createTransactionMessage(transaction, transaction.getDepositAccount()));
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
            case SAVINGS_PAYMENT -> "적금 자동이체";
            case TIME_DEPOSIT_OPENING -> "예금 개설 입금";
        };

        return account.getName() + " 계좌에서 " + amount + "원 " + transactionType + " 거래가 " + status + "되었습니다.";
    }
}
