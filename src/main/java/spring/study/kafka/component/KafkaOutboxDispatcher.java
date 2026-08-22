package spring.study.kafka.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.repository.NotificationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxDispatcher {
    private static final int BATCH_SIZE = 100;

    private final KafkaOutboxEventRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${kafka.outbox.retry-base-delay-ms:5000}")
    private long retryBaseDelayMs = 5000L;
    @Value("${kafka.outbox.retry-max-delay-ms:1800000}")
    private long retryMaxDelayMs = 1_800_000L;

    @Transactional
    public DispatchResult publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<KafkaOutboxEvent> events = outboxRepository.findNextBatchForUpdate(now, PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) return DispatchResult.empty();

        int processedCount = 0;
        for (KafkaOutboxEvent event : events) {
            try {
                Object payload = resolvePayload(event);
                if (payload == null) {
                    outboxRepository.delete(event);
                    processedCount++;
                    continue;
                }
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                        .get(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                outboxRepository.delete(event);
                processedCount++;
            } catch (Exception exception) {
                LocalDateTime retryAt = LocalDateTime.now().plusNanos(calculateRetryDelayMillis(event) * 1_000_000L);
                event.recordFailure(exception.getMessage(), retryAt);
                LocalDateTime nextRetryAt = event.isDeadLettered() ? null : retryAt;
                log.warn("Kafka outbox publish failed. eventId={}, attempt={}, retryAt={}", event.getId(), event.getAttemptCount(), nextRetryAt, exception);
                return DispatchResult.failed(processedCount, nextRetryAt);
            }
        }

        return DispatchResult.completed(processedCount, events.size() == BATCH_SIZE);
    }

    private Object resolvePayload(KafkaOutboxEvent event) throws Exception {
        return switch (event.resolvedPayloadType()) {
            case CHAT_MESSAGE -> objectMapper.readValue(event.getPayload(), ChatMessageRequestDto.class);
            case NOTIFICATION -> notificationRepository.findById(Long.valueOf(event.getPayload())).orElse(null);
        };
    }

    private long calculateRetryDelayMillis(KafkaOutboxEvent event) {
        int exponent = Math.min(event.getAttemptCount(), 20);
        long multiplier = 1L << exponent;
        long delay = retryBaseDelayMs > retryMaxDelayMs / multiplier ? retryMaxDelayMs : retryBaseDelayMs * multiplier;
        return Math.min(delay, retryMaxDelayMs);
    }

    public record DispatchResult(int processedCount, boolean batchFull, boolean failed, LocalDateTime retryAt) {
        public static DispatchResult empty() {
            return new DispatchResult(0, false, false, null);
        }

        public static DispatchResult completed(int processedCount, boolean batchFull) {
            return new DispatchResult(processedCount, batchFull, false, null);
        }

        public static DispatchResult failed(int processedCount, LocalDateTime retryAt) {
            return new DispatchResult(processedCount, false, true, retryAt);
        }

        public boolean hasWork() {
            return processedCount > 0 || failed;
        }
    }
}
