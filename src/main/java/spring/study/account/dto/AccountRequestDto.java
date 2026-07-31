package spring.study.account.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountType;
import spring.study.member.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class AccountRequestDto {
    private String account;
    private long amount;
    private String name;
    private AccountType accountType;
    private Member member;

    @Builder
    public AccountRequestDto(String account, long amount, String name, AccountType accountType, Member member) {
        this.account = account;
        this.amount = amount;
        this.name = name;
        this.accountType = accountType;
        this.member = member;
    }

    public Account toEntity() {
        return Account.builder()
                .account(account)
                .amount(amount)
                .name(name)
                .accountType(accountType)
                .member(member)
                .build();
    }
}
