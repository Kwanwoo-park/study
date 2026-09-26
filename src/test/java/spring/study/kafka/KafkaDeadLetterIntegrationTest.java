package spring.study.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.component.KafkaEventLogRetryListener;
import spring.study.kafka.config.*;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.repository.*;
import spring.study.kafka.service.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaDeadLetterIntegrationTest {
    @Test
    void failedBatchAndMalformedBytesReachDltAndCanBeRepublishedWithoutChangingPayload() throws Exception {
        EmbeddedKafkaBroker broker = new EmbeddedKafkaBroker(1, true, 1, KafkaTopics.ALL.toArray(String[]::new));
        broker.brokerProperty("auto.create.topics.enable", "false");
        broker.afterPropertiesSet();
        var mapper = new ObjectMapper().findAndRegisterModules();
        var properties = new KafkaOperationsProperties(); properties.setConsumerRetries(1); properties.setConsumerRetryDelayMs(100);
        var producers = new KafkaProducerConfig(broker.getBrokersAsString(), mapper);
        var producer = producers.kafkaTemplate(); var recovery = producers.kafkaRecoveryTemplate();
        var logs = mock(IntegrationEventLogService.class);
        var consumers = new KafkaConsumerConfig(broker.getBrokersAsString(), "reliability-check", mapper, 10, 1, 100, properties);
        var recoverer = consumers.deadLetterPublishingRecoverer(recovery);
        var container = consumers.chatBatchKafkaListenerContainerFactory(new KafkaEventLogRetryListener(logs), recoverer).createContainer(KafkaTopics.CHAT);
        container.getContainerProperties().setShutdownTimeout(5000);
        container.getContainerProperties().setMessageListener((BatchMessageListener<String, Object>) records -> { throw new IllegalStateException("simulated DB failure"); });
        var factory = new DefaultKafkaConsumerFactory<>(KafkaTestUtils.consumerProps("dlt-verification", "false", broker), new StringDeserializer(), new ByteArrayDeserializer());
        try (var dlt = factory.createConsumer(); var original = factory.createConsumer("source-replay-verification", "replay");
             Admin admin = Admin.create(Map.of("bootstrap.servers", broker.getBrokersAsString()))) {
            broker.consumeFromAnEmbeddedTopic(dlt, KafkaTopics.CHAT_DLT);
            container.start(); ContainerTestUtils.waitForAssignment(container, 1);
            var message = ChatMessageRequestDto.builder().id("msg-1").roomId("room-1").message("example").build();
            var sent = new ProducerRecord<String, Object>(KafkaTopics.CHAT, "room-1", message);
            sent.headers().add(KafkaTopics.EVENT_ID_HEADER, "outbox-42".getBytes(StandardCharsets.UTF_8));
            producer.send(sent).get(10, TimeUnit.SECONDS);
            byte[] malformed = "{bad-json".getBytes(StandardCharsets.UTF_8);
            recovery.send(KafkaTopics.CHAT, "room-2", malformed).get(10, TimeUnit.SECONDS);
            List<ConsumerRecord<String, byte[]>> records = new ArrayList<>();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (records.size() < 2 && System.nanoTime() < deadline) dlt.poll(Duration.ofMillis(300)).forEach(records::add);
            assertThat(records).hasSize(2);
            assertThat(records).anySatisfy(record -> assertThat(record.value()).containsExactly(malformed));
            var valid = records.stream().filter(record -> record.key().equals("room-1")).findFirst().orElseThrow();
            assertThat(mapper.readTree(valid.value()).get("id").asText()).isEqualTo("msg-1");
            assertThat(valid.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_OFFSET)).isNotNull();
            assertThat(valid.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE)).isNull();

            var repository = mock(KafkaDeadLetterRepository.class);
            var archived = org.mockito.ArgumentCaptor.forClass(KafkaDeadLetter.class);
            new KafkaDeadLetterService(repository, logs).archive(valid);
            verify(repository).save(archived.capture());
            var entry = archived.getValue(); entry.requestReplay(1L);
            container.stop();
            broker.consumeFromAnEmbeddedTopic(original, true, KafkaTopics.CHAT);
            original.assignment().forEach(original::position);
            when(repository.findReplayBatch(any(), any(), any())).thenReturn(List.of(entry));
            new KafkaDeadLetterReplayService(repository, recovery, logs, properties).publishPending();
            assertThat(entry.getStatus()).as(entry.getErrorType()).isEqualTo(KafkaDeadLetter.Status.REPLAYED);
            var replayed = KafkaTestUtils.getSingleRecord(original, KafkaTopics.CHAT, Duration.ofSeconds(10));
            assertThat(replayed.value()).containsExactly(valid.value());
            assertThat(entry.getStatus()).isEqualTo(KafkaDeadLetter.Status.REPLAYED);
            assertThat(new String(replayed.headers().lastHeader(KafkaTopics.EVENT_ID_HEADER).value(), StandardCharsets.UTF_8)).isEqualTo("outbox-42");
            var outbox = mock(KafkaOutboxEventRepository.class);
            var overview = new KafkaMonitoringService(admin, properties, outbox, repository, "reliability-check").overview();
            assertThat(overview.brokerError()).isNull();
            assertThat(overview.topics()).hasSize(4);
            assertThat(overview.lags()).hasSize(4);
        } finally {
            container.stop(); producer.destroy(); recovery.destroy();
            ((DefaultKafkaProducerFactory<?, ?>) producer.getProducerFactory()).destroy();
            ((DefaultKafkaProducerFactory<?, ?>) recovery.getProducerFactory()).destroy();
            broker.destroy();
        }
    }
}
