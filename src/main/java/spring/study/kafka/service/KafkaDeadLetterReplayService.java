package spring.study.kafka.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import static spring.study.admin.entity.IntegrationEventLog.*;

@Service
public class KafkaDeadLetterReplayService {
    private final KafkaDeadLetterRepository repository;
    private final KafkaTemplate<String, Object> template;
    private final IntegrationEventLogService logs;
    private final KafkaOperationsProperties properties;
    public KafkaDeadLetterReplayService(KafkaDeadLetterRepository repository,
            @Qualifier("kafkaRecoveryTemplate") KafkaTemplate<String, Object> template,
            IntegrationEventLogService logs, KafkaOperationsProperties properties) {
        this.repository = repository; this.template = template; this.logs = logs; this.properties = properties;
    }
    @Transactional
    public void publishPending() {
        for (var entry : repository.findReplayBatch(Status.REPLAY_PENDING, LocalDateTime.now(), PageRequest.of(0, 10))) {
            try {
                ProducerRecord<String, Object> record = new ProducerRecord<>(entry.getSourceTopic(), entry.getEventKey(), entry.getPayload());
                record.headers().add("__TypeId__", KafkaTopics.payloadClass(entry.getSourceTopic()).getBytes(StandardCharsets.UTF_8));
                if (entry.getEventId() != null) record.headers().add(KafkaTopics.EVENT_ID_HEADER, entry.getEventId().getBytes(StandardCharsets.UTF_8));
                template.send(record).get(15, TimeUnit.SECONDS);
                entry.replayed();
                logs.afterCommit(Route.kafka(entry.getSourceTopic()), Operation.REPLAY, Outcome.SUCCESS, entry.getId(), 1, entry.getAttemptCount(), null);
            } catch (Exception error) {
                entry.replayFailed(error);
                logs.afterCommit(Route.kafka(entry.getSourceTopic()), Operation.REPLAY,
                        entry.getStatus() == Status.REPLAY_FAILED ? Outcome.FAILED : Outcome.RETRY_SCHEDULED,
                        entry.getId(), 1, entry.getAttemptCount(), error);
                if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                break;
            }
        }
    }
    @Transactional
    public void cleanup() {
        repository.deleteReplayedBefore(Status.REPLAYED, LocalDateTime.now().minusDays(properties.getReplayHistoryDays()));
    }
}
