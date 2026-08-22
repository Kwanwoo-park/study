package spring.study.kafka.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaOutboxDispatcherTest {
    @Test
    void failedPublishShouldPersistFiveSecondBackoff() {
        KafkaOutboxEventRepository repository = mock(KafkaOutboxEventRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaOutboxDispatcher dispatcher = new KafkaOutboxDispatcher(repository, notificationRepository, kafkaTemplate, new ObjectMapper());
        KafkaOutboxEvent event = new KafkaOutboxEvent("topic", "room-1", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}");
        LocalDateTime beforeDispatch = LocalDateTime.now();
        when(repository.findNextBatchForUpdate(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenThrow(new IllegalStateException("Kafka unavailable"));

        KafkaOutboxDispatcher.DispatchResult result = dispatcher.publishPendingEvents();

        assertTrue(result.failed());
        assertNotNull(result.retryAt());
        assertTrue(result.retryAt().isAfter(beforeDispatch.plusSeconds(4)));
        assertTrue(result.retryAt().isBefore(beforeDispatch.plusSeconds(7)));
        assertTrue(event.getNextAttemptAt().isEqual(result.retryAt()));
        assertFalse(event.isDeadLettered());
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
