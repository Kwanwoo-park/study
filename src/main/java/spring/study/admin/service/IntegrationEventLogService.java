package spring.study.admin.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.IntegrationEventLogItem;
import spring.study.admin.dto.IntegrationEventLogResponse;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.entity.IntegrationEventLog.*;
import spring.study.admin.dto.IntegrationEventLogDiagnostics;
import spring.study.admin.dto.IntegrationEventLogEntry;
import spring.study.admin.repository.IntegrationEventLogRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationEventLogService {
    @Value("${admin.event-log.enabled:}")
    private boolean enabled;
    @Value("${admin.event-log.capacity:}")
    private int capacity;
    @Value("${admin.event-log.retention-days:}")
    private int retentionDays;

    private final IntegrationEventLogRepository integrationEventLogRepository;

    private ArrayBlockingQueue<IntegrationEventLogEntry> queue;
    private String instanceId;
    private AtomicLong dropped;
    private AtomicLong storageFailures;
    private AtomicLong nextWarningAt;
    private List<IntegrationEventLogEntry> pending;

    private volatile int pendingCount;
    private volatile LocalDateTime lastSavedAt;

    @PostConstruct
    void init() {
        queue = new ArrayBlockingQueue<>(Math.max(1, Math.min(capacity, 30)));
        instanceId = UUID.randomUUID().toString();
        dropped = new AtomicLong();
        storageFailures = new AtomicLong();
        nextWarningAt = new AtomicLong();
        pending = List.of();
    }

    public void record(Route route, Operation operation, Outcome outcome, Long referenceId, int count, Throwable error) {
        record(route, operation, outcome, referenceId, count, null, null, error);
    }

    public void record(Route route, Operation operation, Outcome outcome, Long referenceId, int count, Integer attempt, Long subscribers, Throwable error) {
        if (!enabled) return;

        IntegrationEventLogEntry entry = entry(route, operation, outcome, referenceId, count, attempt, subscribers, error);

        offer(entry);
    }

    public void afterCommit(Route route, Operation operation, Outcome outcome, Long referenceId, int count,
                            Integer attempt, Throwable error) {
        if (!enabled) return;

        IntegrationEventLogEntry entry = entry(route, operation, outcome, referenceId, count, attempt, null, error);

        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    offer(entry);
                }
            });
        else
            offer(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void flush() {
        if (!enabled) return;

        if (pending.isEmpty()) {
            List<IntegrationEventLogEntry> batch = new ArrayList<>(100);

            queue.drainTo(batch, 100);
            pending = batch;
            pendingCount = batch.size();

            if (pending.isEmpty()) return;
        }

        try {
            integrationEventLogRepository.saveAllAndFlush(pending.stream().map(IntegrationEventLog::new).toList());

            pending = List.of();
            pendingCount = 0;
            lastSavedAt = LocalDateTime.now();
        } catch (RuntimeException exception) {
            storageFailures.incrementAndGet(); warn("storage-unavailable");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanup() {
        if (!enabled) return;

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            for (int batch = 0; batch < 10; batch++) {
                List<Long> ids = integrationEventLogRepository.findExpiredIds(cutoff, PageRequest.of(0, 1000));

                if (!ids.isEmpty())
                    integrationEventLogRepository.deleteAllByIdInBatch(ids);

                if (ids.size() < 1000)
                    break;
            }
        } catch (RuntimeException exception) {
            storageFailures.incrementAndGet();
            warn("cleanup-unavailable");
        }
    }

    @Transactional(readOnly = true)
    public IntegrationEventLogItem findById(long id) {
        LocalDateTime since = LocalDateTime.now().minusDays(retentionDays);

        return integrationEventLogRepository.findById(id)
                .filter(log -> !log.getOccurredAt().isBefore(since))
                .map(IntegrationEventLogItem::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기록이 없거나 보관 기간이 지나 삭제되었습니다."));
    }

    @Transactional(readOnly = true)
    public IntegrationEventLogResponse find(Broker broker, Operation operation, Outcome outcome, Long beforeId, int hours) {
        if ((beforeId != null && beforeId < 1) || hours < 1 || hours > 720) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간과 페이지를 확인해 주세요");
        }

        LocalDateTime since = LocalDateTime.now().minusHours(Math.min(hours, retentionDays * 24L));
        List<IntegrationEventLog> logs = integrationEventLogRepository.search(since, broker, operation, outcome, beforeId, PageRequest.of(0, 51));
        List<IntegrationEventLogItem> entries = logs.stream().limit(50).map(IntegrationEventLogItem::new).toList();
        Long next = logs.size() > 50 ? entries.get(entries.size() - 1).id() : null;

        return new IntegrationEventLogResponse(entries, next, retentionDays, diagnostics());
    }

    private IntegrationEventLogDiagnostics diagnostics() {
        return new IntegrationEventLogDiagnostics(enabled, instanceId, queue.size() + pendingCount, dropped.get(), storageFailures.get(), lastSavedAt);
    }

    private IntegrationEventLogEntry entry(Route route, Operation operation, Outcome outcome, Long referenceId, int count, Integer attempt, Long subscribers,Throwable error) {
        String errorType = error == null ? null : NestedExceptionUtils.getMostSpecificCause(error).getClass().getName();

        if (errorType != null && errorType.length() > 255)
            errorType = errorType.substring(0, 255);

        return new IntegrationEventLogEntry(LocalDateTime.now(), instanceId, route, operation, outcome, referenceId, Math.max(0, count), attempt, subscribers, errorType);
    }

    private void offer(IntegrationEventLogEntry entry) {
        if (!queue.offer(entry)) {
            dropped.incrementAndGet();
            warn("buffer-full");
        }
    }

    private void warn(String reason) {
        long now = System.currentTimeMillis(), previous = nextWarningAt.get();

        if (now >= previous && nextWarningAt.compareAndSet(previous, now + 60000)) {
            log.warn("Integration event log degraded: reason={}, dropped={}, storageFailures={}", reason, dropped.get(), storageFailures.get());
        }
    }
}
