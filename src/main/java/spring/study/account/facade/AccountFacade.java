package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.account.dto.*;
import spring.study.account.entity.*;
import spring.study.account.service.AccountService;
import spring.study.account.service.AccountTransactionService;
import spring.study.account.service.AccountNotificationService;
import spring.study.common.component.TransactionRunner;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AccountFacade {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;
    private final AccountNotificationService accountNotificationService;
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
                accountNotificationService.notifyTransaction(accountTransactionService.recordOpening(source, account, amount));
            }
            return ResponseEntity.ok(Map.of("result", 10L, "accountNum", account.getAccount(), "accountType", account.getAccountType()));
        });
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getList(Member member) {
        return ResponseEntity.ok(Map.of("list", accountService.findByMember(member), "name", member.getName(), "result", 10L));
    }

    public ResponseEntity<?> tranAccount(AccountTranDto dto, Member member) {
        return write(() -> {
            validateAmount(dto);
            Account[] accounts = lockPair(dto.getAccount(), dto.getTranAccount());
            Account source = accounts[0], target = accounts[1];
            requireOwner(source, member);
            LocalDateTime time = LocalDateTime.now();
            accountService.tranAccount(source, target, dto.getAmount(), time);
            accountNotificationService.notifyTransaction(accountTransactionService.recordTransfer(source, target, dto.getAmount(), time));
            return ResponseEntity.ok(Map.of("result", 10L, "message", "정상적으로 이체되었습니다"));
        });
    }

    public ResponseEntity<?> deposit(AccountTranDto dto, Member member) {
        return write(() -> {
            validateAmount(dto);
            Account account = lock(dto.getAccount());
            requireOwner(account, member);
            LocalDateTime time = LocalDateTime.now();
            accountService.deposit(account, dto.getAmount(), time);
            accountNotificationService.notifyTransaction(accountTransactionService.recordDeposit(account, dto.getAmount(), time));
            return ResponseEntity.ok(Map.of("result", 10L, "amount", account.getAmount(), "message", "정상적으로 입금되었습니다"));
        });
    }

    public ResponseEntity<?> changeAccountName(AccountRequestDto dto, Member member) {
        return write(() -> {
            Account account = lock(dto.getAccount());
            requireOwner(account, member);
            accountService.changeAccountName(account.getAccount(), dto.getName());
            return ResponseEntity.ok(Map.of("result", 10L));
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
            return ResponseEntity.ok(Map.of("result", 10L));
        });
    }

    public ResponseEntity<?> terminate(String number, String settlementNumber, Member member) {
        return write(() -> {
            if (settlementNumber == null || settlementNumber.isBlank())
                throw new IllegalArgumentException("정산받을 입출금 계좌를 선택해주세요");
            Account[] accounts = lockPair(number, settlementNumber);
            LocalDateTime time = LocalDateTime.now();
            AccountSettlementResult settlement = accountService.terminateInterestAccount(accounts[0], accounts[1], member, time);
            accountTransactionService.recordSettlement(accounts[0], accounts[1], settlement, time)
                    .forEach(accountNotificationService::notifyTransaction);
            return ResponseEntity.ok(Map.of("result", 10L, "settlement", settlement,
                    "message", settlement.matured() ? "만기 해지가 완료되었습니다" : "중도 해지가 완료되었습니다"));
        });
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
}
