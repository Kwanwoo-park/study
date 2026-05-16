package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.Account;
import spring.study.member.entity.Member;

@Getter
@NoArgsConstructor
public class AccountResponseDto {
    private String account;
    private long amount;
    private String name;
    private Member member;

    public AccountResponseDto(Account entity) {
        this.account = entity.getAccount();
        this.amount = entity.getAmount();
        this.name = entity.getName();
        this.member = entity.getMember();
    }

    @Override
    public String toString() {
        return "AccountResponseDto{" +
                "account='" + account + '\'' +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                ", member=" + member +
                '}';
    }
}
