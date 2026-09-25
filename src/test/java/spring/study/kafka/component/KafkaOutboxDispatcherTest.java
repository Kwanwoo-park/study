package spring.study.kafka.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.repository.NotificationRepository;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaOutboxDispatcherTest {
    @Test
    void successfulBrokerSendIsRecordedBeforeDeletingOutbox() {
        KafkaOutboxEventRepository repository = mock(KafkaOutboxEventRepository.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
        KafkaOutboxDispatcher dispatcher = new KafkaOutboxDispatcher(repository, mock(NotificationRepository.class), kafka, new ObjectMapper(), logs);
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}");
        when(repository.findNextBatchForUpdate(any(), any())).thenReturn(List.of(event));
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        assertFalse(dispatcher.publishPendingEvents().failed());
        var order = org.mockito.Mockito.inOrder(kafka, logs, repository);
        order.verify(kafka).send(anyString(), anyString(), any());
        order.verify(logs).record(Route.CHAT, Operation.PUBLISH, Outcome.SUCCESS, null, 1, 1, null, null);
        order.verify(repository).delete(event);
    }

    @Test
    void exhaustedOutboxPublishesADeadLetterLogRatherThanAnotherRetry() {
        KafkaOutboxEventRepository repository = mock(KafkaOutboxEventRepository.class);
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
        KafkaOutboxDispatcher dispatcher = new KafkaOutboxDispatcher(repository, mock(NotificationRepository.class), kafka, new ObjectMapper(), logs);
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}");
        for (int i = 0; i < 9; i++) event.recordFailure("failed", LocalDateTime.now());
        when(repository.findNextBatchForUpdate(any(), any())).thenReturn(List.of(event));
        RuntimeException failure = new IllegalStateException("Kafka down");
        when(kafka.send(anyString(), anyString(), any())).thenThrow(failure);
        assertTrue(dispatcher.publishPendingEvents().failed());
        verify(logs).afterCommit(Route.CHAT, Operation.PUBLISH, Outcome.DEAD_LETTER, null, 1, 10, failure);
    }

    @Test
    void failedPublishShouldPersistFiveSecondBackoff() {
        KafkaOutboxEventRepository repository = mock(KafkaOutboxEventRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
        KafkaOutboxDispatcher dispatcher = new KafkaOutboxDispatcher(repository, notificationRepository, kafkaTemplate, new ObjectMapper(), logs);
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room-1", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}");
        LocalDateTime beforeDispatch = LocalDateTime.now();
        when(repository.findNextBatchForUpdate(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(event));
        IllegalStateException failure = new IllegalStateException("Kafka unavailable");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(failure);

        KafkaOutboxDispatcher.DispatchResult result = dispatcher.publishPendingEvents();

        assertTrue(result.failed());
        assertNotNull(result.retryAt());
        assertTrue(result.retryAt().isAfter(beforeDispatch.plusSeconds(4)));
        assertTrue(result.retryAt().isBefore(beforeDispatch.plusSeconds(7)));
        assertTrue(event.getNextAttemptAt().isEqual(result.retryAt()));
        assertFalse(event.isDeadLettered());
        verify(logs).afterCommit(Route.CHAT, Operation.PUBLISH, Outcome.RETRY_SCHEDULED, null, 1, 1, failure);
    }

    @Test
    void tenthFailureShouldMoveEventToDeadLetter() {
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room-1", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}");

        for (int attempt = 0; attempt < 10; attempt++) {
            event.recordFailure("failure", LocalDateTime.now().plusMinutes(30));
        }

        assertTrue(event.isDeadLettered());
        assertTrue(event.getNextAttemptAt() == null);
    }
}
