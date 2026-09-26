package spring.study.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

@EnableKafka
@Configuration
public class KafkaProducerConfig {
    private final String server;
    private final ObjectMapper mapper;

    public KafkaProducerConfig(@Value("${bootstrap-servers}") String server, ObjectMapper mapper) {
        this.server = server;
        this.mapper = mapper;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>(mapper);
        return new DefaultKafkaProducerFactory<>(producerSettings(),
                new StringSerializer(),
                jsonSerializer);
    }

    private Map<String, Object> producerSettings() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return config;
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, Object> recoveryProducerFactory() {
        Map<Class<?>, Serializer<?>> serializers = new LinkedHashMap<>();
        serializers.put(byte[].class, new ByteArraySerializer());
        serializers.put(Object.class, new JsonSerializer<>(mapper));
        Map<String, Object> settings = producerSettings();
        settings.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        settings.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        settings.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        return new DefaultKafkaProducerFactory<>(settings, new StringSerializer(), new DelegatingByTypeSerializer(serializers, true));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaRecoveryTemplate() {
        return new KafkaTemplate<>(recoveryProducerFactory());
    }
}
