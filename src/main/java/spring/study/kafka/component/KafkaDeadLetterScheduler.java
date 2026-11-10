package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.kafka.facade.KafkaDeadLetterReplayFacade;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "spring.study.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDeadLetterScheduler {
    private final KafkaDeadLetterReplayFacade facade;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "kafka-dead-letter-replay"); thread.setDaemon(true); return thread;
    });

    @PostConstruct
    public void start() {
        long replayPollMs = 5000;
        executor.scheduleWithFixedDelay(this::replay, 10000, replayPollMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    public void replay() {
        try {
            facade.publishPending();
        } catch (RuntimeException error) {
            log.warn("Kafka replay queue unavailable. errorType={}", error.getClass().getSimpleName());
        }
    }

    @Scheduled(cron = "${kafka.operations.cleanup-cron:0 30 3 * * *}")
    public void cleanup() {
        facade.cleanup();
    }
}
