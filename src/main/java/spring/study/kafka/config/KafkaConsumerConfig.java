package spring.study.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
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

    public KafkaConsumerConfig(@Value("${bootstrap-servers}") String server,
                               @Value("${group-id}") String groupId,
                               ObjectMapper mapper,
                               @Value("${chat.kafka.batch.max-records:100}") int chatBatchMaxRecords,
                               @Value("${chat.kafka.batch.fetch-min-bytes:32768}") int chatBatchFetchMinBytes,
                               @Value("${chat.kafka.batch.fetch-max-wait-ms:1000}") int chatBatchFetchMaxWaitMs) {
        this.server = server;
        this.groupId = groupId;
        this.mapper = mapper;
        this.chatBatchMaxRecords = chatBatchMaxRecords;
        this.chatBatchFetchMinBytes = chatBatchFetchMinBytes;
        this.chatBatchFetchMaxWaitMs = chatBatchFetchMaxWaitMs;
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
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.getContainerProperties().setMissingTopicsFatal(false);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> chatBatchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatBatchConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.getContainerProperties().setMissingTopicsFatal(false);

        return factory;
    }
}
