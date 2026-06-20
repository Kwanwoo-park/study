package spring.study.account.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.account.entity.Account;
import spring.study.account.entity.AccountTransaction;
import spring.study.account.repository.AccountTransactionRepository;

@Service
@RequiredArgsConstructor
public class AccountTransactionService {
    private static final int PAGE_SIZE = 20;

    private final AccountService accountService;
    private final AccountTransactionRepository accountTransactionRepository;

    @Transactional
    public AccountTransaction save(AccountTransaction accountTransaction) {
        return accountTransactionRepository.save(accountTransaction);
    }

    public Page<AccountTransaction> findByAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccountOrDepositAccount(
                account,
                account,
                createPageRequest(page)
        );
    }

    public Page<AccountTransaction> findByWithdrawalAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByWithdrawalAccount(account, createPageRequest(page));
    }

    public Page<AccountTransaction> findByDepositAccount(String accountNumber, int page) {
        Account account = accountService.findByAccount(accountNumber);

        return accountTransactionRepository.findByDepositAccount(account, createPageRequest(page));
    }

    private PageRequest createPageRequest(int page) {
        return PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by(Sort.Order.desc("transactionTime"), Sort.Order.desc("id"))
        );
    }
}
