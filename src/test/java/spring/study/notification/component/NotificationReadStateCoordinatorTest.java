package spring.study.notification.component;

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

class NotificationReadStateCoordinatorTest {
    private final EmitterService emitters = mock(EmitterService.class);
    private final NotificationRealtimePublisher publisher = mock(NotificationRealtimePublisher.class);
    private final NotificationReadStateCoordinator service = new NotificationReadStateCoordinator(emitters, publisher);
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
    void readStateChangesAfterCommitAndRedisFailureRetriesOnlyUntilSuccess() {
        doThrow(new IllegalStateException("offline")).doNothing().when(publisher).publishReadState(read);
        service.afterCommit(read);
        verifyNoInteractions(emitters, publisher);

        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
        verify(emitters).applyReadState(read);
        verify(publisher).publishReadState(read);

        service.retryPending();
        service.retryPending();
        verify(publisher, times(2)).publishReadState(read);
        verify(emitters).applyReadState(read);
    }
}
