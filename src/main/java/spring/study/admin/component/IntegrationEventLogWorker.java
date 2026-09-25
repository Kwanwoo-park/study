package spring.study.admin.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import spring.study.admin.service.IntegrationEventLogService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.study.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationEventLogWorker {
    private final IntegrationEventLogService service;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "integration-event-log"); thread.setDaemon(true); return thread;
    });
    @PostConstruct
    public void start() {
        executor.scheduleWithFixedDelay(service::flush, 1, 1, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(service::cleanup, 60, 60, TimeUnit.SECONDS);
    }
    @PreDestroy
    public void stop() {
        executor.execute(service::flush);
        executor.shutdown();
        try { if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException exception) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
