package spring.study.account.facade;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.account.entity.AccountType;
import spring.study.account.dto.AccountCreateRequestDto;
import spring.study.account.service.AccountService;
import spring.study.member.entity.Member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

class AccountFacadeCreateTest {
    private final AccountService accountService = mock(AccountService.class);
    private final AccountFacade accountFacade = new AccountFacade(accountService);

    @Test
    void interestAccountCreationShouldReturnBadRequestWithoutCheckingAccount() {
        Member member = Member.builder().id(1L).email("member@test.com").build();
        when(accountService.createAccount(eq(member), any(AccountCreateRequestDto.class)))
                .thenThrow(new IllegalArgumentException(
                        "예적금 계좌를 만들려면 먼저 활성 상태의 입출금 계좌가 필요합니다"
                ));

        ResponseEntity<?> response = accountFacade.create(member, AccountType.INSTALLMENT_SAVINGS);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
