package spring.study.account.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.account.dto.AccountRequestDto;
import spring.study.account.dto.AccountTranDto;
import spring.study.account.dto.AccountTerminationRequestDto;
import spring.study.account.facade.AccountFacade;
import spring.study.account.facade.AccountTransactionFacade;
import spring.study.account.entity.AccountType;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/account")
public class AccountApiController {
    private final JwtManager jwtManager;
    private final AccountFacade accountFacade;
    private final AccountTransactionFacade accountTransactionFacade;
    private final CommonFacade commonFacade;

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(
            @RequestParam(defaultValue = "DEPOSIT_WITHDRAWAL") AccountType accountType,
            HttpServletRequest request
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.create(member, accountType);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getAccountList(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.getList(member);
    }

    @PatchMapping("/tran")
    public ResponseEntity<?> tranAccount(@RequestBody AccountTranDto dto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.tranAccount(dto, member);
    }

    @PatchMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AccountRequestDto dto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.deposit(dto, member);
    }

    @PatchMapping("/change/name")
    public ResponseEntity<?> changeAccountName(@RequestBody AccountRequestDto dto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.changeAccountName(dto, member);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestParam String account, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.delete(account, member);
    }

    @PostMapping("/{account}/terminate")
    public ResponseEntity<?> terminateAccount(@PathVariable String account,
                                              @RequestBody AccountTerminationRequestDto dto,
                                              HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountFacade.terminate(account, dto.getSettlementAccount(), member);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam String account,
            @RequestParam(defaultValue = "0") int page,
            HttpServletRequest request
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountTransactionFacade.getListByAccount(account, page, member);
    }

    @PatchMapping("/transactions/{id}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return accountTransactionFacade.cancel(id, member);
    }
}
