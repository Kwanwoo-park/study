package spring.study.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountTranDto {
    private String account;
    private String tranAccount;
    private long amount;
}
