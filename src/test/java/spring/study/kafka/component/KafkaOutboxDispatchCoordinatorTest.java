package spring.study.kafka.component;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOutboxDispatchCoordinatorTest {
    @Test
    void immediateRequestShouldDispatchWithoutWaitingForSafetyPoll() {
        KafkaOutboxDispatcher dispatcher = mock(KafkaOutboxDispatcher.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        KafkaOutboxDispatchCoordinator coordinator = new KafkaOutboxDispatchCoordinator(dispatcher, scheduler, 30_000L, 300_000L);
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        when(dispatcher.publishPendingEvents()).thenReturn(KafkaOutboxDispatcher.DispatchResult.empty());

        coordinator.requestImmediateDispatch();
        verify(scheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getValue().run();

        verify(dispatcher).publishPendingEvents();
    }

    @Test
    void emptySafetyPollShouldIncreaseFromThirtyToSixtySeconds() {
        KafkaOutboxDispatcher dispatcher = mock(KafkaOutboxDispatcher.class);
        TaskScheduler scheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        KafkaOutboxDispatchCoordinator coordinator = new KafkaOutboxDispatchCoordinator(dispatcher, scheduler, 30_000L, 300_000L);
        List<Runnable> scheduledTasks = new ArrayList<>();
        List<Instant> scheduledTimes = new ArrayList<>();
        doAnswer(invocation -> {
            scheduledTasks.add(invocation.getArgument(0));
            scheduledTimes.add(invocation.getArgument(1));
            return future;
        }).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        when(dispatcher.publishPendingEvents()).thenReturn(KafkaOutboxDispatcher.DispatchResult.empty());

        coordinator.start();
        scheduledTasks.get(0).run();
        Runnable secondPoll = scheduledTasks.get(1);
        Instant secondPollAt = scheduledTimes.get(1);
        assertTrue(Duration.between(Instant.now(), secondPollAt).toSeconds() >= 29L);

        secondPoll.run();
        Instant thirdPollAt = scheduledTimes.get(2);
        assertTrue(Duration.between(Instant.now(), thirdPollAt).toSeconds() >= 59L);
    }
}
