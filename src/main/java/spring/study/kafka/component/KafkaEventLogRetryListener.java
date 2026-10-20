package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.stereotype.Component;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;

@Component
@RequiredArgsConstructor
public class KafkaEventLogRetryListener implements RetryListener {
    private final IntegrationEventLogService eventLogs;

    @Override
    public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int attempt) {
        record(record, exception, attempt, IntegrationEventLogOutcome.FAILED);
    }
    @Override
    public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
        record(record, exception, null, IntegrationEventLogOutcome.DLT_PUBLISHED);
    }
    @Override
    public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
        record(record, failure, null, IntegrationEventLogOutcome.FAILED);
    }
    private void record(ConsumerRecord<?, ?> record, Exception error, Integer attempt, IntegrationEventLogOutcome outcome) {
        eventLogs.record(IntegrationEventLogRoute.kafka(record.topic()), IntegrationEventLogOperation.CONSUMER_RETRY, outcome, null, 1, attempt, null, error);
    }
    @Override
    public void failedDelivery(ConsumerRecords<?, ?> records, Exception exception, int attempt) {
        batch(records, exception, attempt, IntegrationEventLogOutcome.FAILED);
    }
    @Override
    public void recovered(ConsumerRecords<?, ?> records, Exception exception) {
        batch(records, exception, null, IntegrationEventLogOutcome.DLT_PUBLISHED);
    }
    @Override
    public void recoveryFailed(ConsumerRecords<?, ?> records, Exception original, Exception failure) {
        batch(records, failure, null, IntegrationEventLogOutcome.FAILED);
    }
    private void batch(ConsumerRecords<?, ?> records, Exception error, Integer attempt, IntegrationEventLogOutcome outcome) {
        // Batch metadata only; never inspect record.value(), key(), or headers().
        IntegrationEventLogRoute route = records.isEmpty() ? IntegrationEventLogRoute.UNKNOWN_KAFKA : IntegrationEventLogRoute.kafka(records.iterator().next().topic());
        eventLogs.record(route, IntegrationEventLogOperation.CONSUMER_RETRY, outcome, null, records.count(), attempt, null, error);
    }
}
