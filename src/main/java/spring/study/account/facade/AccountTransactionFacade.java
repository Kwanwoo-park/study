package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountTransactionResponseDto;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.service.AccountService;
import spring.study.account.service.AccountTransactionService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTransactionFacade {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;

    public ResponseEntity<?> getListByAccount(String accountNumber, int page, Member member) {
        ResponseEntity<?> validation = validateOwner(accountNumber, member);
        if (validation != null) {
            return validation;
        }

        Page<AccountTransaction> transactionPage = accountTransactionService.findByAccount(accountNumber, page);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", transactionPage.getContent()
                        .stream()
                        .map(AccountTransactionResponseDto::new)
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
            AccountTransaction cancelTransaction = accountTransactionService.cancelTransaction(transactionId, member);

            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "transaction", new AccountTransactionResponseDto(cancelTransaction),
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
}
