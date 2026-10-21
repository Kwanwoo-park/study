package spring.study.notification.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import spring.study.common.service.EmitterService;
import spring.study.notification.dto.NotificationReadState;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class NotificationReadStateServiceTest {
    private final EmitterService emitters = mock(EmitterService.class);
    private final NotificationRealtimePublisher publisher = mock(NotificationRealtimePublisher.class);
    private final NotificationReadStateService service = new NotificationReadStateService(emitters, publisher);
    private final NotificationReadState read = new NotificationReadState(7L, 1L, null, null, LocalDateTime.now());

    @BeforeEach
    void begin() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    @AfterEach
    void end() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void cacheAndOtherServersChangeOnlyAfterCommit() {
        service.afterCommit(read);
        verifyNoInteractions(emitters, publisher);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(emitters).applyReadState(read);
        verify(publisher).publishReadState(read);
    }

    @Test
    void rollbackDoesNotChangeCacheOrPublishReadState() {
        service.afterCommit(read);
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(emitters, publisher);
    }

    @Test
    void redisFailureIsRetriedWithoutFailingTheCommittedRequest() {
        doThrow(new IllegalStateException("offline")).doNothing().when(publisher).publishReadState(read);
        service.afterCommit(read);
        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        service.retryPending();
        service.retryPending();
        verify(publisher, times(2)).publishReadState(read);
        verify(emitters).applyReadState(read);
    }
}
