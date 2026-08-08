package spring.study.account.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.account.service.AccountService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccountMaturityScheduler {
    private final AccountService accountService;

    @Scheduled(cron = "${study.account.maturity-cron:0 0 * * * *}")
    public void markMaturedAccounts() {
        accountService.markMaturedAccounts(LocalDateTime.now());
    }
}
