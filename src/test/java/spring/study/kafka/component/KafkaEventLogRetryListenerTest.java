package spring.study.kafka.component;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import spring.study.admin.service.IntegrationEventLogService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static spring.study.admin.entity.IntegrationEventLog.*;

class KafkaEventLogRetryListenerTest {
    @Test
    void observesPreListenerFailuresAndExhaustedRecoveryWithoutPayloads() {
        IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
        KafkaEventLogRetryListener listener = new KafkaEventLogRetryListener(logs);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic2", 0, 10, "private-key", "private-payload");
        Exception error = new IllegalArgumentException("bad serialized payload");
        listener.failedDelivery(record, error, 3);
        listener.recovered(record, error);
        verify(logs).record(Route.NOTIFICATION, Operation.CONSUMER_RETRY, Outcome.FAILED, null, 1, 3, null, error);
        verify(logs).record(Route.NOTIFICATION, Operation.CONSUMER_RETRY, Outcome.DISCARDED, null, 1, null, null, error);
    }
    @Test
    void batchErrorIsOneMetadataRecordWithBatchSize() {
        IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
        KafkaEventLogRetryListener listener = new KafkaEventLogRetryListener(logs);
        ConsumerRecords<String, String> records = new ConsumerRecords<>(Map.of(new TopicPartition("topic", 0),
                List.of(new ConsumerRecord<>("topic", 0, 1, "key", "secret"), new ConsumerRecord<>("topic", 0, 2, "key", "secret"))));
        Exception error = new IllegalStateException("failed");
        listener.failedDelivery(records, error, 2);
        verify(logs).record(Route.CHAT, Operation.CONSUMER_RETRY, Outcome.FAILED, null, 2, 2, null, error);
    }
}
