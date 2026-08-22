package spring.study.kafka.component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "spring.study.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaOutboxDispatchCoordinator {
    private final KafkaOutboxDispatcher kafkaOutboxDispatcher;
    private final TaskScheduler taskScheduler;
    private final Duration initialSafetyDelay;
    private final Duration maxSafetyDelay;
    private final AtomicBoolean immediateDispatchRequested = new AtomicBoolean();
    private final AtomicBoolean immediateDispatchScheduled = new AtomicBoolean();
    private final Object scheduleMonitor = new Object();
    private volatile Duration nextEmptySafetyDelay;
    private volatile Instant blockedUntil = Instant.MIN;
    private ScheduledFuture<?> safetyFuture;
    private ScheduledFuture<?> retryFuture;
    private Instant retryScheduledAt;

    public KafkaOutboxDispatchCoordinator(KafkaOutboxDispatcher kafkaOutboxDispatcher, @Qualifier("kafkaOutboxTaskScheduler") TaskScheduler taskScheduler, @Value("${kafka.outbox.safety-poll-initial-ms:30000}") long initialSafetyDelayMs, @Value("${kafka.outbox.safety-poll-max-ms:300000}") long maxSafetyDelayMs) {
        this.kafkaOutboxDispatcher = kafkaOutboxDispatcher;
        this.taskScheduler = taskScheduler;
        this.initialSafetyDelay = Duration.ofMillis(Math.max(initialSafetyDelayMs, 1000L));
        this.maxSafetyDelay = Duration.ofMillis(Math.max(maxSafetyDelayMs, this.initialSafetyDelay.toMillis()));
        this.nextEmptySafetyDelay = this.initialSafetyDelay;
    }

    @PostConstruct
    public void start() {
        scheduleSafetyPoll(Duration.ZERO);
    }

    @PreDestroy
    public void stop() {
        synchronized (scheduleMonitor) {
            if (safetyFuture != null) safetyFuture.cancel(false);
            if (retryFuture != null) retryFuture.cancel(false);
        }
    }

    public void requestImmediateDispatch() {
        immediateDispatchRequested.set(true);
        scheduleImmediateDispatchIfNeeded();
    }

    private void scheduleImmediateDispatchIfNeeded() {
        Instant now = Instant.now();
        if (now.isBefore(blockedUntil)) {
            scheduleRetry(blockedUntil);
            return;
        }
        if (!immediateDispatchScheduled.compareAndSet(false, true)) return;

        taskScheduler.schedule(this::runImmediateDispatch, now);
    }

    private void runImmediateDispatch() {
        try {
            do {
                immediateDispatchRequested.set(false);
                KafkaOutboxDispatcher.DispatchResult result = kafkaOutboxDispatcher.publishPendingEvents();
                handleDispatchResult(result);
                if (result.failed() && result.retryAt() != null) break;
                if (result.batchFull() || (result.failed() && result.retryAt() == null)) immediateDispatchRequested.set(true);
            } while (immediateDispatchRequested.get());
        } finally {
            immediateDispatchScheduled.set(false);
            if (immediateDispatchRequested.get()) scheduleImmediateDispatchIfNeeded();
        }
    }

    private void runSafetyPoll() {
        synchronized (scheduleMonitor) {
            safetyFuture = null;
        }

        boolean hasWork = false;
        if (Instant.now().isBefore(blockedUntil)) {
            scheduleRetry(blockedUntil);
        } else {
            KafkaOutboxDispatcher.DispatchResult result = kafkaOutboxDispatcher.publishPendingEvents();
            hasWork = result.hasWork();
            handleDispatchResult(result);
            if (result.batchFull() || (result.failed() && result.retryAt() == null)) requestImmediateDispatch();
        }

        Duration nextDelay = hasWork ? initialSafetyDelay : nextEmptySafetyDelay;
        nextEmptySafetyDelay = hasWork ? initialSafetyDelay : doubleDelay(nextEmptySafetyDelay);
        scheduleSafetyPoll(nextDelay);
    }

    private void handleDispatchResult(KafkaOutboxDispatcher.DispatchResult result) {
        if (!result.failed()) {
            blockedUntil = Instant.MIN;
            return;
        }
        if (result.retryAt() == null) {
            blockedUntil = Instant.MIN;
            return;
        }

        Duration retryDelay = Duration.between(LocalDateTime.now(), result.retryAt());
        blockedUntil = Instant.now().plus(retryDelay.isNegative() ? Duration.ZERO : retryDelay);
        scheduleRetry(blockedUntil);
    }

    private void scheduleSafetyPoll(Duration delay) {
        synchronized (scheduleMonitor) {
            if (safetyFuture != null && !safetyFuture.isDone()) return;
            safetyFuture = taskScheduler.schedule(this::runSafetyPoll, Instant.now().plus(delay));
        }
    }

    private void scheduleRetry(Instant retryAt) {
        synchronized (scheduleMonitor) {
            if (retryFuture != null && !retryFuture.isDone() && retryScheduledAt != null && !retryAt.isBefore(retryScheduledAt)) return;
            if (retryFuture != null) retryFuture.cancel(false);
            retryScheduledAt = retryAt;
            retryFuture = taskScheduler.schedule(this::runRetry, retryAt);
        }
    }

    private void runRetry() {
        synchronized (scheduleMonitor) {
            retryFuture = null;
            retryScheduledAt = null;
        }
        requestImmediateDispatch();
    }

    private Duration doubleDelay(Duration delay) {
        long doubledMillis = Math.min(delay.toMillis() * 2L, maxSafetyDelay.toMillis());
        return Duration.ofMillis(doubledMillis);
    }
}
