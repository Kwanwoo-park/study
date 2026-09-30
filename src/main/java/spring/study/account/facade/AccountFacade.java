package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.account.dto.*;
import spring.study.account.entity.*;
import spring.study.account.service.AccountService;
import spring.study.account.service.AccountTransactionService;
import spring.study.common.component.TransactionRunner;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AccountFacade {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;
    private final NotificationService notificationService;
    private final TransactionRunner transactions;

    public ResponseEntity<?> create(Member member, AccountType type) {
        AccountCreateRequestDto request = new AccountCreateRequestDto();
        request.setAccountType(type);

        return create(member, request);
    }

    public ResponseEntity<?> create(Member member, AccountCreateRequestDto input) {
        AccountCreateRequestDto request = input == null ? new AccountCreateRequestDto() : input;

        return write(() -> {
            AccountType type = request.getAccountType() == null ? AccountType.DEPOSIT_WITHDRAWAL : request.getAccountType();
            Account source = switch (type) {
                case INSTALLMENT_SAVINGS -> accountService.resolveSavingsSourceAccount(member, request);
                case TIME_DEPOSIT -> accountService.resolveTimeDepositSourceAccount(member, request);
                case DEPOSIT_WITHDRAWAL -> null;
            };
            Account account = accountService.createAccount(member, request, source);

            if (type.isInterestBearing()) {
                long amount = type == AccountType.INSTALLMENT_SAVINGS ? request.getMonthlySavingsAmount() : request.getTimeDepositAmount();
                notifyMember(accountTransactionService.recordOpening(source, account, amount));
            }

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "accountNum", account.getAccount(),
                    "accountType", account.getAccountType()
            ));
        });
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getList(Member member) {
        return ResponseEntity.ok(Map.of(
                "list", accountService.findByMember(member),
                "name", member.getName(),
                "result", 10L
        ));
    }

    public ResponseEntity<?> tranAccount(AccountTranDto dto, Member member) {
        return write(() -> {
            validateAmount(dto);

            Account[] accounts = lockPair(dto.getAccount(), dto.getTranAccount());
            Account source = accounts[0], target = accounts[1];

            requireOwner(source, member);

            LocalDateTime time = LocalDateTime.now();

            accountService.tranAccount(source, target, dto.getAmount(), time);
            notifyMember(accountTransactionService.recordTransfer(source, target, dto.getAmount(), time));

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "message", "정상적으로 이체되었습니다"
            ));
        });
    }

    public ResponseEntity<?> deposit(AccountTranDto dto, Member member) {
        return write(() -> {
            validateAmount(dto);

            Account account = lock(dto.getAccount());

            requireOwner(account, member);

            LocalDateTime time = LocalDateTime.now();

            accountService.deposit(account, dto.getAmount(), time);
            notifyMember(accountTransactionService.recordDeposit(account, dto.getAmount(), time));

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "amount", account.getAmount(),
                    "message", "정상적으로 입금되었습니다"
            ));
        });
    }

    public ResponseEntity<?> changeAccountName(AccountRequestDto dto, Member member) {
        return write(() -> {
            Account account = lock(dto.getAccount());

            requireOwner(account, member);
            accountService.changeAccountName(account.getAccount(), dto.getName());

            return ResponseEntity.ok(Map.of(
                    "result", 10L
            ));
        });
    }

    public ResponseEntity<?> delete(String number, Member member) {
        return write(() -> {
            Account account = lock(number);

            requireOwner(account, member);

            if (account.getAccountType() == AccountType.DEPOSIT_WITHDRAWAL && accountService.hasActiveSavingsUsingSource(account))
                throw new IllegalArgumentException("적금 자동이체에 사용 중인 입출금 계좌는 삭제할 수 없습니다");

            if (account.isInterestBearing() && account.getAccountStatus() != AccountStatus.TERMINATED)
                throw new IllegalArgumentException("예적금 계좌는 해지 정산 기능을 이용해주세요");

            accountService.deleteByAccount(number);

            return ResponseEntity.ok(Map.of(
                    "result", 10L
            ));
        });
    }

    public ResponseEntity<?> terminate(String number, String settlementNumber, Member member) {
        return write(() -> {
            if (settlementNumber == null || settlementNumber.isBlank())
                throw new IllegalArgumentException("정산받을 입출금 계좌를 선택해주세요");

            Account[] accounts = lockPair(number, settlementNumber);
            LocalDateTime time = LocalDateTime.now();
            AccountSettlementResult settlement = accountService.terminateInterestAccount(accounts[0], accounts[1], member, time);

            accountTransactionService.recordSettlement(accounts[0], accounts[1], settlement, time).forEach(this::notifyMember);

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "settlement", settlement,
                    "message", settlement.matured() ? "만기 해지가 완료되었습니다" : "중도 해지가 완료되었습니다"
            ));
        });
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getListByAccount(String accountNumber, int page, Member member) {
        ResponseEntity<?> validation = validateOwner(accountNumber, member);
        if (validation != null) return validation;

        Account account = accountService.findByAccount(accountNumber);

        Page<AccountTransaction> transactionPage = accountTransactionService.findByAccount(account, page);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", transactionPage.getContent().stream()
                        .map(transaction -> new AccountTransactionResponseDto(transaction, member))
                        .toList(),
                "page", transactionPage.getNumber(),
                "size", transactionPage.getSize(),
                "totalPages", transactionPage.getTotalPages(),
                "totalElements", transactionPage.getTotalElements(),
                "hasNext", transactionPage.hasNext(),
                "hasPrevious", transactionPage.hasPrevious()
        ));
    }

    public ResponseEntity<?> cancel(Long transactionId, Member member) {
        try {
            AccountTransactionResponseDto cancelTransaction = transactions.write(() -> {
                AccountTransaction transaction = accountTransactionService.findUpdate(transactionId);

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

                AccountTransactionService.Cancellation canceled = accountTransactionService.cancelTransaction(transaction, reversalWithdrawalAccount, reversalDepositAccount, cancellationTime);
                notifyMember(canceled.original());
                return new AccountTransactionResponseDto(canceled.cancellation());
            });

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "transaction", cancelTransaction,
                    "message", "거래가 취소되었습니다"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }
    }

    private Account lock(String number) {
        if (number == null || number.isBlank()) throw new IllegalArgumentException("존재하지 않는 계좌입니다");

        return accountService.findByAccountForUpdate(number);
    }

    private Account[] lockPair(String source, String target) {
        if (source == null || source.isBlank() || target == null || target.isBlank())
            throw new IllegalArgumentException("존재하지 않는 계좌입니다");

        if (source.equals(target)) throw new IllegalArgumentException("같은 계좌로 이체할 수 없습니다");

        boolean sourceFirst = source.compareTo(target) < 0;

        Account first = lock(sourceFirst ? source : target);
        Account second = lock(sourceFirst ? target : source);

        return sourceFirst ? new Account[]{first, second} : new Account[]{second, first};
    }

    private ResponseEntity<?> validateOwner(String accountNumber, Member member) {
        if (accountNumber == null || accountNumber.isBlank()) return accountNotFound();

        if (!accountService.existsByAccount(accountNumber)) return accountNotFound();

        Account account = accountService.findByAccount(accountNumber);

        if (!account.getMember().getId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -10L,
                    "message", "본인 계좌만 조회할 수 있습니다"
            ));
        }

        return null;
    }

    private ResponseEntity<?> accountNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "result", -10L,
                "message", "존재하지 않는 계좌입니다"
        ));
    }

    private void requireOwner(Account account, Member member) {
        if (member == null || account.getMember() == null || !account.getMember().getId().equals(member.getId()))
            throw new SecurityException("본인 계좌만 사용할 수 있습니다");
    }

    private void validateAmount(AccountTranDto dto) {
        if (dto == null || dto.getAmount() < 10_000L)
            throw new IllegalArgumentException("거래 금액은 1만원 이상이어야 합니다");
    }

    private ResponseEntity<?> write(Supplier<ResponseEntity<?>> action) {
        try {
            return transactions.write(action);
        } catch (SecurityException error) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", -10L, "message", error.getMessage()));
        } catch (IllegalArgumentException | ArithmeticException error) {
            return ResponseEntity.badRequest().body(Map.of("result", -10L, "message", error.getMessage()));
        }
    }

    private void notifyMember(AccountTransaction transaction) {
        String amount = NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.getAmount());
        String status = transaction.getTransactionStatus() == AccountTransactionStatus.CANCELED ? "취소" : "완료";

        Account account;

        if (transaction.getWithdrawalAccount() != null && transaction.getWithdrawalAccount().getMember() != null) {
            account = transaction.getWithdrawalAccount();
            notificationService.createNotification(
                    account.getMember(),
                    account.getName() + " 계좌에서 " + amount + "원 출금 거래가 " + status  + "되었습니다.",
                    Group.TRAN,
                    account.getAccount()
            );
        }

        if (transaction.getDepositAccount() != null && transaction.getDepositAccount().getMember() != null) {
            account = transaction.getDepositAccount();
            notificationService.createNotification(
                    account.getMember(),
                    account.getName() + " 계좌에서 " + amount + "원 입금 거래가 " + status + "되었습니다.",
                    Group.TRAN,
                    account.getAccount()
            );
        }
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

    private Account accountByNumber(Account[] accounts, Account original) {
        if (original == null) return null;

        for (Account account : accounts) {
            if (account.getAccount().equals(original.getAccount())) return account;
        }

        throw new IllegalStateException("거래 계좌 잠금에 실패했습니다");
    }

    private Account[] lockAccounts(Account first, Account second) {
        if (first == null && second == null) return new Account[0];
        else if (first == null) return new Account[]{accountService.findByAccountForUpdate(second.getAccount())};
        else if (second == null) return new Account[]{accountService.findByAccountForUpdate(first.getAccount())};

        String firstNumber = first.getAccount().compareTo(second.getAccount()) <= 0 ? first.getAccount() : second.getAccount();
        String secondNumber = firstNumber.equals(first.getAccount()) ? second.getAccount() : first.getAccount();

        return new Account[]{ accountService.findByAccountForUpdate(firstNumber), accountService.findByAccountForUpdate(secondNumber) };
    }

    private boolean isAccountOwner(Account account, Long memberId) {
        return account != null && account.getMember() != null && account.getMember().getId().equals(memberId);
    }
}
