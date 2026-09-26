package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.stereotype.Component;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;

@Component
@RequiredArgsConstructor
public class KafkaEventLogRetryListener implements RetryListener {
    private final IntegrationEventLogService eventLogs;

    @Override
    public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int attempt) {
        record(record, exception, attempt, Outcome.FAILED);
    }
    @Override
    public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
        record(record, exception, null, Outcome.DLT_PUBLISHED);
    }
    @Override
    public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
        record(record, failure, null, Outcome.FAILED);
    }
    private void record(ConsumerRecord<?, ?> record, Exception error, Integer attempt, Outcome outcome) {
        eventLogs.record(Route.kafka(record.topic()), Operation.CONSUMER_RETRY, outcome, null, 1, attempt, null, error);
    }
    @Override
    public void failedDelivery(ConsumerRecords<?, ?> records, Exception exception, int attempt) {
        batch(records, exception, attempt, Outcome.FAILED);
    }
    @Override
    public void recovered(ConsumerRecords<?, ?> records, Exception exception) {
        batch(records, exception, null, Outcome.DLT_PUBLISHED);
    }
    @Override
    public void recoveryFailed(ConsumerRecords<?, ?> records, Exception original, Exception failure) {
        batch(records, failure, null, Outcome.FAILED);
    }
    private void batch(ConsumerRecords<?, ?> records, Exception error, Integer attempt, Outcome outcome) {
        // Batch metadata only; never inspect record.value(), key(), or headers().
        Route route = records.isEmpty() ? Route.UNKNOWN_KAFKA : Route.kafka(records.iterator().next().topic());
        eventLogs.record(route, Operation.CONSUMER_RETRY, outcome, null, records.count(), attempt, null, error);
    }
}
