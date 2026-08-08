package spring.study.account.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.account.service.SavingsAutoTransferService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SavingsAutoTransferScheduler {
    private final SavingsAutoTransferService savingsAutoTransferService;

    @Scheduled(cron = "${study.account.savings-transfer-cron:0 5 * * * *}")
    public void processSavingsPayments() {
        LocalDateTime processingTime = LocalDateTime.now();
        LocalDate processingDate = processingTime.toLocalDate();
        savingsAutoTransferService.findDueSavingsAccountNumbers(processingDate)
                .forEach(accountNumber -> {
                    try {
                        savingsAutoTransferService.processDuePayment(
                                accountNumber,
                                processingDate,
                                processingTime
                        );
                    } catch (RuntimeException e) {
                        log.error("적금 자동이체 처리 실패: account={}", accountNumber, e);
                    }
                });
    }
}
