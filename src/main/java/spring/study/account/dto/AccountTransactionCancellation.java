package spring.study.account.dto;

import spring.study.account.entity.AccountTransaction;

public record AccountTransactionCancellation(AccountTransaction original, AccountTransaction cancellation) {}
