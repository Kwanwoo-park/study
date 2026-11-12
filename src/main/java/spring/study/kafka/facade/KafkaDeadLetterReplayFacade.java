package spring.study.kafka.facade;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.service.KafkaDeadLetterService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaDeadLetterReplayFacade {
    private final KafkaDeadLetterService deadLetterService;
    private final IntegrationEventLogService logService;
    private final KafkaTemplate<String, Object> template;

    public KafkaDeadLetterReplayFacade (KafkaDeadLetterService deadLetterService, IntegrationEventLogService logService, @Qualifier("kafkaRecoveryTemplate") KafkaTemplate<String, Object> template) {
        this.deadLetterService = deadLetterService;
        this.logService = logService;
        this.template = template;
    }

    public void publishPending() {
        List<KafkaDeadLetter> entries = deadLetterService.findEntries();

        for (KafkaDeadLetter entry : entries) {
            try {
                ProducerRecord<String, Object> record = new ProducerRecord<>(entry.getSourceTopic(), entry.getEventKey(), entry.getPayload());

                record.headers().add("__TypeId__", KafkaTopics.payloadClass(entry.getSourceTopic()).getBytes(StandardCharsets.UTF_8));

                if (entry.getEventId() != null)
                    record.headers().add(KafkaTopics.EVENT_ID_HEADER, entry.getEventId().getBytes(StandardCharsets.UTF_8));

                template.send(record).get(15, TimeUnit.SECONDS);
                entry.replayed();

                logService.afterCommit(IntegrationEventLogRoute.kafka(entry.getSourceTopic()), IntegrationEventLogOperation.REPLAY, IntegrationEventLogOutcome.SUCCESS, entry.getId(), 1, entry.getAttemptCount(), null);
            } catch (Exception e) {
                entry.replayFailed(e);

                logService.afterCommit(IntegrationEventLogRoute.kafka(entry.getSourceTopic()), IntegrationEventLogOperation.REPLAY,
                        entry.getStatus() == KafkaDeadLetterStatus.REPLAY_FAILED ? IntegrationEventLogOutcome.FAILED : IntegrationEventLogOutcome.RETRY_SCHEDULED,
                        entry.getId(), 1, entry.getAttemptCount(), e);

                if (e instanceof InterruptedException)
                    Thread.currentThread().interrupt();

                break;
            }
        }
    }

    public void cleanup() {
        deadLetterService.cleanup();
    }
}
