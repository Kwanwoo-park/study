package spring.study.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccountMaturityScheduler {
    private final AccountService accountService;

    @Scheduled(cron = "${spring.study.account.maturity-cron:0 0 * * * *}")
    public void markMaturedAccounts() {
        accountService.markMaturedAccounts(LocalDateTime.now());
    }
}
