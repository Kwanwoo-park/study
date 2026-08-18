package spring.study.kafka.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.notification.repository.NotificationRepository;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOutboxDispatcher {
    private final KafkaOutboxEventRepository outboxRepository;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${kafka.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (KafkaOutboxEvent event : outboxRepository.findNextBatchForUpdate(PageRequest.of(0, 100))) {
            try {
                Object payload = resolvePayload(event);
                if (payload == null) {
                    outboxRepository.delete(event);
                    continue;
                }
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload)
                        .get(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                outboxRepository.delete(event);
            } catch (Exception exception) {
                event.recordFailure(exception.getMessage());
                log.warn("Kafka outbox publish will be retried. eventId={}", event.getId(), exception);
                break;
            }
        }
    }

    private Object resolvePayload(KafkaOutboxEvent event) throws Exception {
        return switch (event.resolvedPayloadType()) {
            case CHAT_MESSAGE -> objectMapper.readValue(event.getPayload(), ChatMessageRequestDto.class);
            case NOTIFICATION -> notificationRepository.findById(Long.valueOf(event.getPayload())).orElse(null);
        };
    }
}
