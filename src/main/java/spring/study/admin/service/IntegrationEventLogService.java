package spring.study.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.study.admin.entity.IntegrationEventLog.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class IntegrationEventLogService {
    private final IntegrationEventLogStore store;
    private final ArrayBlockingQueue<Entry> queue;
    private final boolean enabled;
    private final int retentionDays;
    private final String instanceId = UUID.randomUUID().toString();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong storageFailures = new AtomicLong();
    private final AtomicLong nextWarningAt = new AtomicLong();
    private List<Entry> pending = List.of();
    private volatile int pendingCount;
    private volatile LocalDateTime lastSavedAt;

    public IntegrationEventLogService(IntegrationEventLogStore store,
            @Value("${admin.event-log.enabled:true}") boolean enabled,
            @Value("${admin.event-log.capacity:2000}") int capacity,
            @Value("${admin.event-log.retention-days:7}") int retentionDays) {
        this.store = store; this.enabled = enabled;
        this.retentionDays = Math.max(1, Math.min(30, retentionDays));
        this.queue = new ArrayBlockingQueue<>(Math.max(1, Math.min(10000, capacity)));
    }

    public void record(Route route, Operation operation, Outcome outcome, Long referenceId, int count, Throwable error) {
        record(route, operation, outcome, referenceId, count, null, null, error);
    }

    public void record(Route route, Operation operation, Outcome outcome, Long referenceId, int count,
                       Integer attempt, Long subscribers, Throwable error) {
        if (!enabled) return;
        Entry entry = entry(route, operation, outcome, referenceId, count, attempt, subscribers, error);
        offer(entry);
    }

    public void afterCommit(Route route, Operation operation, Outcome outcome, Long referenceId, int count,
                            Integer attempt, Throwable error) {
        if (!enabled) return;
        Entry entry = entry(route, operation, outcome, referenceId, count, attempt, null, error);
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { offer(entry); }
            });
        } else offer(entry);
    }

    private Entry entry(Route route, Operation operation, Outcome outcome, Long referenceId, int count,
                        Integer attempt, Long subscribers, Throwable error) {
        String errorType = error == null ? null : NestedExceptionUtils.getMostSpecificCause(error).getClass().getName();
        if (errorType != null && errorType.length() > 255) errorType = errorType.substring(0, 255);
        return new Entry(LocalDateTime.now(), instanceId, route, operation, outcome, referenceId,
                Math.max(0, count), attempt, subscribers, errorType);
    }

    private void offer(Entry entry) {
        if (!queue.offer(entry)) { dropped.incrementAndGet(); warn("buffer-full"); }
    }

    public synchronized void flush() {
        if (!enabled) return;
        if (pending.isEmpty()) {
            List<Entry> batch = new ArrayList<>(100);
            queue.drainTo(batch, 100); pending = batch; pendingCount = batch.size();
        }
        if (pending.isEmpty()) return;
        try {
            store.append(pending);
            pending = List.of(); pendingCount = 0; lastSavedAt = LocalDateTime.now();
        } catch (RuntimeException exception) {
            storageFailures.incrementAndGet(); warn("storage-unavailable");
        }
    }

    public void cleanup() {
        if (!enabled) return;
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
            for (int batch = 0; batch < 10; batch++) if (store.deleteExpiredBatch(cutoff) < 1000) break;
        } catch (RuntimeException exception) { storageFailures.incrementAndGet(); warn("cleanup-unavailable"); }
    }

    private void warn(String reason) {
        long now = System.currentTimeMillis(), previous = nextWarningAt.get();
        if (now >= previous && nextWarningAt.compareAndSet(previous, now + 60000)) {
            log.warn("Integration event log degraded: reason={}, dropped={}, storageFailures={}", reason, dropped.get(), storageFailures.get());
        }
    }

    public int retentionDays() { return retentionDays; }
    public Diagnostics diagnostics() {
        return new Diagnostics(enabled, instanceId, queue.size() + pendingCount, dropped.get(), storageFailures.get(), lastSavedAt);
    }
    public record Diagnostics(boolean enabled, String instanceId, int pending, long dropped, long storageFailures, LocalDateTime lastSavedAt) {}
}
