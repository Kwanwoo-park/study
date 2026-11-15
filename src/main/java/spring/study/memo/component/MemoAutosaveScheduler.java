package spring.study.memo.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import spring.study.memo.facade.MemoFacade;
import spring.study.memo.config.MemoAutosaveProperties;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j
public class MemoAutosaveScheduler {
    private final MemoFacade memoFacade;
    private final TaskScheduler scheduler;
    private final MemoAutosaveProperties properties;
    private ScheduledFuture<?> task;
    private long nextAttemptAt;
    private int failures;

    public MemoAutosaveScheduler(MemoFacade memoFacade, @Qualifier("memoAutosaveTaskScheduler") TaskScheduler scheduler, MemoAutosaveProperties properties) {
        this.memoFacade = memoFacade;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        task = scheduler.scheduleWithFixedDelay(this::flushDue, Duration.ofMillis(properties.getPollMs()));
    }

    @PreDestroy
    public void stop() {
        if (task != null)
            task.cancel(false);
    }

    public void flushDue() {
        if (System.currentTimeMillis() < nextAttemptAt)
            return;

        try {
            memoFacade.flushDue();
            failures = 0;
        } catch (RuntimeException exception) {
            nextAttemptAt = System.currentTimeMillis() + Math.min(60000, 1000L << Math.min(failures++, 6));

            log.warn("Memo draft queue unavailable; will retry. error={}", exception.getClass().getSimpleName());
        }
    }
}
