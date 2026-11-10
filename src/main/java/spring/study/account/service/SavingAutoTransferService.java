package spring.study.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.account.repository.SavingAutoTransferRepository;
import spring.study.account.entity.Account;
import spring.study.account.entity.SavingsAutoTransfer;

@Service
@RequiredArgsConstructor
public class SavingAutoTransferService {
    private final SavingAutoTransferRepository transferRepository;

    @Transactional(readOnly = true)
    public String findSavingsSourceAccountNumber(String account) {
        if (account == null) return null;

        return transferRepository.findById(account)
                .map(SavingsAutoTransfer::getSourceAccount)
                .map(Account::getAccount)
                .orElse(null);
    }
}
