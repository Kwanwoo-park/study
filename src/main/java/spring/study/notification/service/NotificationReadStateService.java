package spring.study.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.study.common.service.EmitterService;
import spring.study.notification.dto.NotificationReadState;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationReadStateService {
    private final EmitterService emitterService;
    private final NotificationRealtimePublisher publisher;
    private final Map<String, NotificationReadState> pending = new ConcurrentHashMap<>();

    public void afterCommit(NotificationReadState read) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Notification reads must be synchronized inside a transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Update this server before returning the successful read response.
                // Redis failures must not turn an already committed read into an HTTP error.
                emitterService.applyReadState(read);
                pending.merge(read.scopeKey(), read, NotificationReadStateService::latest);
                publishPending(read.scopeKey());
            }
        });
    }

    public void retryPending() {
        emitterService.cleanupEventCache();
        pending.values().removeIf(read -> read.readBefore().isBefore(LocalDateTime.now().minusHours(1)));
        pending.keySet().stream().limit(100).toList().forEach(this::publishPending);
    }

    private void publishPending(String key) {
        NotificationReadState read = pending.get(key);
        if (read == null) return;
        try {
            publisher.publishReadState(read);
            pending.remove(key, read);
        } catch (RuntimeException exception) {
            log.warn("Notification read propagation will be retried. memberId={}, notificationId={}",
                    read.memberId(), read.notificationId(), exception);
        }
    }

    private static NotificationReadState latest(NotificationReadState left, NotificationReadState right) {
        return right.readBefore().isAfter(left.readBefore()) ? right : left;
    }
}
