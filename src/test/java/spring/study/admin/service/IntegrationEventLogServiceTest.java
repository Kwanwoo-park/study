package spring.study.admin.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.study.admin.entity.IntegrationEventLog.Entry;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static spring.study.admin.entity.IntegrationEventLog.*;

class IntegrationEventLogServiceTest {
    private final IntegrationEventLogStore store = mock(IntegrationEventLogStore.class);
    private final IntegrationEventLogService service = new IntegrationEventLogService(store, true, 2, 7);

    @AfterEach
    void clearTransaction() { TransactionSynchronizationManager.clear(); }

    @Test
    void capturesOnlyMetadataAndDoesNotWriteOnBusinessThread() {
        service.record(Route.NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, 8L, 1,
                new IllegalStateException("Bearer secret email@example.test", new IllegalArgumentException("private payload")));
        verifyNoInteractions(store);
        service.flush();
        ArgumentCaptor<List<Entry>> captor = ArgumentCaptor.forClass(List.class);
        verify(store).append(captor.capture());
        Entry entry = captor.getValue().get(0);
        assertThat(entry.errorType()).isEqualTo(IllegalArgumentException.class.getName());
        assertThat(entry.toString()).doesNotContain("secret", "email@example.test", "private payload");
        assertThat(service.diagnostics().pending()).isZero();
        assertThat(service.diagnostics().lastSavedAt()).isNotNull();
    }

    @Test
    void fullBufferDropsOnlyLogsAndRemainsBounded() {
        for (int i = 0; i < 5; i++) service.record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 1, null);
        assertThat(service.diagnostics().pending()).isEqualTo(2);
        assertThat(service.diagnostics().dropped()).isEqualTo(3);
    }

    @Test
    void failedDatabaseWriteIsRetriedWithoutFailingTheCaller() {
        service.record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 2, null);
        doThrow(new IllegalStateException("database down")).doNothing().when(store).append(anyList());
        assertThatCode(service::flush).doesNotThrowAnyException();
        assertThat(service.diagnostics().storageFailures()).isEqualTo(1);
        assertThat(service.diagnostics().pending()).isEqualTo(1);
        service.flush();
        assertThat(service.diagnostics().pending()).isZero();
        verify(store, times(2)).append(anyList());
    }

    @Test
    void queuedEventIsRecordedOnlyAfterCommit() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        service.afterCommit(Route.CHAT, Operation.QUEUE, Outcome.QUEUED, 5L, 1, null, null);
        assertThat(service.diagnostics().pending()).isZero();
        TransactionSynchronization callback = TransactionSynchronizationManager.getSynchronizations().get(0);
        callback.afterCommit();
        assertThat(service.diagnostics().pending()).isEqualTo(1);
    }

    @Test
    void rollbackDoesNotRecordAQueuedEvent() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        service.afterCommit(Route.CHAT, Operation.QUEUE, Outcome.QUEUED, 5L, 1, null, null);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        service.flush();
        verifyNoInteractions(store);
        assertThat(service.diagnostics().pending()).isZero();
    }

    @Test
    void disabledLoggingDoesNotQueuePersistOrCleanup() {
        IntegrationEventLogService disabled = new IntegrationEventLogService(store, false, 2, 7);
        disabled.record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 1, null);
        disabled.flush(); disabled.cleanup();
        verifyNoInteractions(store);
        assertThat(disabled.diagnostics().enabled()).isFalse();
    }

    @Test
    void cleanupFailureDoesNotEscapeAndRetentionIsBounded() {
        when(store.deleteExpiredBatch(any())).thenThrow(new IllegalStateException("down"));
        assertThatCode(service::cleanup).doesNotThrowAnyException();
        assertThat(service.diagnostics().storageFailures()).isEqualTo(1);
        assertThat(new IntegrationEventLogService(store, true, 2, 100).retentionDays()).isEqualTo(30);
    }
}
