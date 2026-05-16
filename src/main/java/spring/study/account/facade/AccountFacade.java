package spring.study.account.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.account.dto.AccountRequestDto;
import spring.study.account.dto.AccountResponseDto;
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
                "result", account.getAccount()
        ));
    }

    public ResponseEntity<?> getList(Member member) {
        return ResponseEntity.ok(Map.of(
                "list", accountService.findByMember(member).stream().map(AccountResponseDto::new).toList(),
                "result", 10L
        ));
    }

    public ResponseEntity<?> addAmount(AccountRequestDto dto) {
        accountService.addAmount(dto.getAccount(), dto.getAmount());

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> subAmount(AccountRequestDto dto) {
        accountService.subAmount(dto.getAccount(), dto.getAmount());

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> delete(String account) {
        accountService.deleteByAccount(account);

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }
}
