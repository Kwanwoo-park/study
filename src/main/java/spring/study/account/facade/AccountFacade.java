package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountRequestDto;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.dto.AccountSettlementResult;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.account.entity.AccountStatus;
import spring.study.account.service.AccountService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountFacade {
    private final AccountService accountService;

    public ResponseEntity<?> create(Member member, AccountType accountType) {
        AccountCreateRequestDto requestDto = new AccountCreateRequestDto();
        requestDto.setAccountType(accountType);
        return create(member, requestDto);
    }

    public ResponseEntity<?> create(Member member, AccountCreateRequestDto requestDto) {
        Account account;
        try {
            account = accountService.createAccount(member, requestDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "accountNum", account.getAccount(),
                "accountType", account.getAccountType()
        ));
    }

    public ResponseEntity<?> getList(Member member) {
        return ResponseEntity.ok(Map.of(
                "list", accountService.findByMember(member).stream().map(AccountResponseDto::new).toList(),
                "name", member.getName(),
                "result", 10L
        ));
    }

    public ResponseEntity<?> tranAccount(AccountTranDto dto, Member member) {
        if (dto.getTranAccount() == null || dto.getTranAccount().isBlank()) {
            return accountNotFound();
        }

        if (accountService.existsByAccount(dto.getTranAccount())) {
            return accountNotFound();
        }

        ResponseEntity<?> validation = validateOwner(dto.getAccount(), member);
        if (validation != null) {
            return validation;
        }

        Account account;

        try {
            account = accountService.tranAccount(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "amount", account.getAmount(),
                "message", "정상적으로 이체되었습니다"
        ));
    }

    public ResponseEntity<?> deposit(AccountRequestDto dto, Member member) {
        ResponseEntity<?> validation = validateOwner(dto.getAccount(), member);
        if (validation != null) {
            return validation;
        }

        Account account;

        try {
            account = accountService.deposit(dto.getAccount(), dto.getAmount());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "amount", account.getAmount(),
                "message", "정상적으로 입금되었습니다"
        ));
    }

    public ResponseEntity<?> changeAccountName(AccountRequestDto dto, Member member) {
        ResponseEntity<?> validation = validateOwner(dto.getAccount(), member);
        if (validation != null) {
            return validation;
        }

        accountService.changeAccountName(dto.getAccount(), dto.getName());

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> delete(String account, Member member) {
        ResponseEntity<?> validation = validateOwner(account, member);
        if (validation != null) {
            return validation;
        }

        Account target = accountService.findByAccount(account);
        if (target.getAccountType() == AccountType.DEPOSIT_WITHDRAWAL
                && accountService.hasActiveSavingsUsingSource(target)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "적금 자동이체에 사용 중인 입출금 계좌는 삭제할 수 없습니다"
            ));
        }
        if (target.isInterestBearing() && target.getAccountStatus() != AccountStatus.TERMINATED) {
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

    public ResponseEntity<?> terminate(String accountNumber,
                                       String settlementAccountNumber,
                                       Member member) {
        try {
            AccountSettlementResult settlement = accountService.terminateInterestAccount(
                    accountNumber,
                    settlementAccountNumber,
                    member
            );

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

    private ResponseEntity<?> accountNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "result", -10L,
                "message", "존재하지 않는 계좌입니다"
        ));
    }
}
