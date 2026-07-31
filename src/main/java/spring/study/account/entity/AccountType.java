package spring.study.account.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum AccountType {
    DEPOSIT_WITHDRAWAL("입출금", "Kwanwoo site checking account", BigDecimal.ZERO),
    INSTALLMENT_SAVINGS("적금", "Kwanwoo site savings account", new BigDecimal("0.05")),
    TIME_DEPOSIT("예금", "Kwanwoo site time deposit account", new BigDecimal("0.035"));

    private final String displayName;
    private final String defaultAccountName;
    private final BigDecimal annualInterestRate;

    public boolean isInterestBearing() {
        return annualInterestRate.signum() > 0;
    }
}
