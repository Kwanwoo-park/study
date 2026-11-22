package spring.study.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.component.KafkaOutboxDispatcher;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.dto.KafkaOutboxFailure;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.entity.KafkaOutboxPayloadType;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.kafka.service.KafkaOutboxEventService;
import spring.study.notification.service.NotificationService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("infrastructure")
@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Import({KafkaOutboxDispatcher.class, KafkaOutboxEventService.class, KafkaOutboxIntegrationTest.BrokerConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class KafkaOutboxIntegrationTest {
    @Autowired private KafkaOutboxEventRepository repository;
    @Autowired private KafkaOutboxEventService outbox;
    @Autowired private KafkaOutboxDispatcher dispatcher;
    @Autowired private EmbeddedKafkaBroker broker;
    @MockBean private NotificationService notifications;
    @MockBean private IntegrationEventLogService logs;

    @Test
    void stoppedBrokerLeavesRecoverableFailureAndReplayUsesOriginalEventId() throws Exception {
        KafkaOutboxEvent event = KafkaOutboxEvent.builder()
                .topic(KafkaTopics.CHAT).eventKey("test-room").payloadType(KafkaOutboxPayloadType.CHAT_MESSAGE)
                .payload("""
                        {"id":"message-id","roomId":"test-room","type":"TALK","message":"example"}
                        """).build();
        for (int attempt = 0; attempt < 9; attempt++) event.recordFailure("previous attempt", LocalDateTime.now().minusSeconds(1));
        Long id = repository.save(event).getId();

        var kafka = broker.getKafkaServer(0);
        kafka.shutdown();
        kafka.awaitShutdown();

        assertThat(dispatcher.publishPendingEvents().failed()).isTrue();
        assertThat(outbox.failures(null).entries()).extracting(KafkaOutboxFailure::id).containsExactly(id);
        assertThat(repository.findById(id)).get().satisfies(failed -> assertThat(failed.getAttemptCount()).isEqualTo(10));

        broker.restart(0);
        outbox.requeueFailed(id);
        assertThat(dispatcher.publishPendingEvents().processedCount()).isEqualTo(1);
        assertThat(repository.findById(id)).isEmpty();

        var properties = KafkaTestUtils.consumerProps("infrastructure-test", "false", broker);
        try (var consumer = new KafkaConsumer<>(properties, new StringDeserializer(), new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(consumer, KafkaTopics.CHAT);
            var record = KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.CHAT, Duration.ofSeconds(10));
            assertThat(record.key()).isEqualTo("test-room");
            assertThat(record.value()).contains("message-id");
            assertThat(new String(record.headers().lastHeader(KafkaTopics.EVENT_ID_HEADER).value(), StandardCharsets.UTF_8))
                    .isEqualTo("outbox-" + id);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BrokerConfig {
        @Bean
        EmbeddedKafkaBroker broker() {
            return new EmbeddedKafkaBroker(1, false, KafkaTopics.CHAT)
                    .kafkaPorts(TestSocketUtils.findAvailableTcpPort());
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        DefaultKafkaProducerFactory<String, Object> producer(EmbeddedKafkaBroker broker, ObjectMapper objectMapper) {
            return new DefaultKafkaProducerFactory<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                    ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000,
                    ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000,
                    ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 4000
            ), new StringSerializer(), new JsonSerializer<>(objectMapper));
        }

        @Bean
        KafkaTemplate<String, Object> kafkaTemplate(DefaultKafkaProducerFactory<String, Object> producer) {
            return new KafkaTemplate<>(producer);
        }
    }
}
