package spring.study.kafka.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import spring.study.kafka.config.KafkaTopics;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.repository.NotificationRepository;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.dto.KafkaDispatchResult;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;

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
    private final IntegrationEventLogService eventLogs;
    @Value("${kafka.outbox.retry-base-delay-ms:5000}")
    private long retryBaseDelayMs = 5000L;
    @Value("${kafka.outbox.retry-max-delay-ms:1800000}")
    private long retryMaxDelayMs = 1_800_000L;

    @Transactional
    public KafkaDispatchResult publishPendingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<KafkaOutboxEvent> events = outboxRepository.findNextBatchForUpdate(now, PageRequest.of(0, BATCH_SIZE));
        if (events.isEmpty()) return KafkaDispatchResult.empty();

        int processedCount = 0;
        for (KafkaOutboxEvent event : events) {
            try {
                Object payload = resolvePayload(event);
                if (payload == null) {
                    outboxRepository.delete(event);
                    eventLogs.afterCommit(IntegrationEventLogRoute.kafka(event.getTopic()), IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.SKIPPED, event.getId(), 1, event.getAttemptCount() + 1, null);
                    processedCount++;
                    continue;
                }
                ProducerRecord<String, Object> record = new ProducerRecord<>(event.getTopic(), event.getEventKey(), payload);
                record.headers().add(KafkaTopics.EVENT_ID_HEADER, ("outbox-" + event.getId()).getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record)
                        .get(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                eventLogs.record(IntegrationEventLogRoute.kafka(event.getTopic()), IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.SUCCESS, event.getId(), 1, event.getAttemptCount() + 1, null, null);
                outboxRepository.delete(event);
                processedCount++;
            } catch (Exception exception) {
                LocalDateTime retryAt = LocalDateTime.now().plusNanos(calculateRetryDelayMillis(event) * 1_000_000L);
                event.recordFailure(exception.getMessage(), retryAt);
                LocalDateTime nextRetryAt = event.isDeadLettered() ? null : retryAt;
                eventLogs.afterCommit(IntegrationEventLogRoute.kafka(event.getTopic()), IntegrationEventLogOperation.PUBLISH,
                        event.isDeadLettered() ? IntegrationEventLogOutcome.DEAD_LETTER : IntegrationEventLogOutcome.RETRY_SCHEDULED,
                        event.getId(), 1, event.getAttemptCount(), exception);
                log.warn("Kafka outbox publish failed. eventId={}, attempt={}, retryAt={}", event.getId(), event.getAttemptCount(), nextRetryAt, exception);
                return KafkaDispatchResult.failed(processedCount, nextRetryAt);
            }
        }

        return KafkaDispatchResult.completed(processedCount, events.size() == BATCH_SIZE);
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

}
