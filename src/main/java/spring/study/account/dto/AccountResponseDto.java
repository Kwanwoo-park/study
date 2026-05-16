package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.account.entity.Account;

@Getter
@NoArgsConstructor
public class AccountResponseDto {
    private String account;
    private long amount;
    private String name;

    public AccountResponseDto(Account entity) {
        this.account = entity.getAccount();
        this.amount = entity.getAmount();
        this.name = entity.getName();
    }

    @Override
    public String toString() {
        return "AccountResponseDto{" +
                "account='" + account + '\'' +
                ", amount=" + amount +
                ", name='" + name + '\'' +
                '}';
    }
}
