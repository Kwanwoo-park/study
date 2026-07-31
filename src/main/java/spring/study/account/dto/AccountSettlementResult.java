package spring.study.account.dto;

public record AccountSettlementResult(
        String terminatedAccount,
        String settlementAccount,
        long principal,
        long interest,
        long settlementAmount,
        boolean matured
) {
}
