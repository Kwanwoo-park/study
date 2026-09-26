package spring.study.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.util.backoff.FixedBackOff;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import spring.study.kafka.component.KafkaEventLogRetryListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    private final String server;
    private final String groupId;
    private final ObjectMapper mapper;
    private final int chatBatchMaxRecords;
    private final int chatBatchFetchMinBytes;
    private final int chatBatchFetchMaxWaitMs;
    private final KafkaOperationsProperties operations;

    public KafkaConsumerConfig(@Value("${bootstrap-servers}") String server,
                               @Value("${group-id}") String groupId,
                               ObjectMapper mapper,
                               @Value("${chat.kafka.batch.max-records}") int chatBatchMaxRecords,
                               @Value("${chat.kafka.batch.fetch-min-bytes}") int chatBatchFetchMinBytes,
                               @Value("${chat.kafka.batch.fetch-max-wait-ms}") int chatBatchFetchMaxWaitMs,
                               KafkaOperationsProperties operations) {
        this.server = server;
        this.groupId = groupId;
        this.mapper = mapper;
        this.chatBatchMaxRecords = chatBatchMaxRecords;
        this.chatBatchFetchMinBytes = chatBatchFetchMinBytes;
        this.chatBatchFetchMaxWaitMs = chatBatchFetchMaxWaitMs;
        this.operations = operations;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        return createConsumerFactory(baseConsumerConfig());
    }

    @Bean
    public ConsumerFactory<String, Object> chatBatchConsumerFactory() {
        Map<String, Object> config = baseConsumerConfig();
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, chatBatchMaxRecords);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, chatBatchFetchMinBytes);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, chatBatchFetchMaxWaitMs);

        return createConsumerFactory(config);
    }

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        return config;
    }

    private ConsumerFactory<String, Object> createConsumerFactory(Map<String, Object> config) {
        // JsonDeserializer
        JsonDeserializer<Object> jsonDeserializer =
                new JsonDeserializer<>(Object.class, mapper);
        jsonDeserializer.addTrustedPackages("*");

        // ErrorHandlingDeserializer
        ErrorHandlingDeserializer<Object> errorDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                errorDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(KafkaEventLogRetryListener eventLogs, DeadLetterPublishingRecoverer recoverer) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.getContainerProperties().setMissingTopicsFatal(false);
        factory.setConcurrency(operations.getNotificationConcurrency());
        factory.setCommonErrorHandler(observedErrorHandler(eventLogs, recoverer));

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> chatBatchKafkaListenerContainerFactory(KafkaEventLogRetryListener eventLogs, DeadLetterPublishingRecoverer recoverer) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatBatchConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        factory.setConcurrency(operations.getChatConcurrency());
        factory.setCommonErrorHandler(observedErrorHandler(eventLogs, recoverer));

        return factory;
    }

    private DefaultErrorHandler observedErrorHandler(KafkaEventLogRetryListener eventLogs, DeadLetterPublishingRecoverer recoverer) {
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(operations.getConsumerRetryDelayMs(), operations.getConsumerRetries()));
        handler.setRetryListeners(eventLogs);
        return handler;
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            @Qualifier("kafkaRecoveryTemplate") KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template, (record, error) -> {
            if (!KafkaTopics.SOURCES.contains(record.topic())) throw new IllegalArgumentException("Unknown source topic");
            return new TopicPartition(record.topic() + ".DLT", -1);
        });
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(15));
        recoverer.setAppendOriginalHeaders(false);
        recoverer.setExceptionHeadersCreator((headers, error, key, names) ->
                headers.add(KafkaHeaders.DLT_EXCEPTION_FQCN, error.getClass().getName().getBytes(StandardCharsets.UTF_8)));
        return recoverer;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> deadLetterKafkaListenerContainerFactory() {
        Map<String, Object> config = baseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + ".dead-letter-store");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new ByteArrayDeserializer()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // A DB outage must never discard the only replayable copy from the DLT archive.
        DefaultErrorHandler archiveErrors = new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        archiveErrors.setClassifications(Map.of(), true);
        factory.setCommonErrorHandler(archiveErrors);
        return factory;
    }
}
