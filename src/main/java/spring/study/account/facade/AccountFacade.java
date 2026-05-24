package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountRequestDto;
import spring.study.account.dto.AccountResponseDto;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.entity.Account;
import spring.study.account.service.AccountService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountFacade {
    private final AccountService accountService;

    public ResponseEntity<?> create(Member member) {
        Account account = accountService.createAccount(member);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "accountNum", account.getAccount()
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

        accountService.deleteByAccount(account);

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
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
