package spring.study.kafka.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class KafkaDeadLetterReplayServiceTest {
    @Test
    void replayKeepsRawBytesAndEventIdentityAndRecordsPublishCompletion() {
        var repository = mock(KafkaDeadLetterRepository.class);
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        var service = new KafkaDeadLetterReplayService(repository, template, mock(IntegrationEventLogService.class), new KafkaOperationsProperties());
        byte[] payload = "{\"id\":\"m1\"}".getBytes(StandardCharsets.UTF_8);
        var event = new KafkaDeadLetter("topic", 0, 3, "room", "outbox-7", payload, "Error");
        event.requestReplay(1L);
        when(repository.findReplayBatch(any(), any(), any())).thenReturn(List.of(event));
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        service.publishPending();
        ArgumentCaptor<ProducerRecord<String, Object>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(sent.capture());
        assertThat(sent.getValue().value()).isSameAs(payload);
        assertThat(sent.getValue().key()).isEqualTo("room");
        assertThat(new String(sent.getValue().headers().lastHeader(KafkaTopics.EVENT_ID_HEADER).value(), StandardCharsets.UTF_8)).isEqualTo("outbox-7");
        assertThat(event.getStatus()).isEqualTo(Status.REPLAYED);
    }
    @Test
    void brokerFailureDoesNotClaimSuccessfulReplay() {
        var repository = mock(KafkaDeadLetterRepository.class);
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        var service = new KafkaDeadLetterReplayService(repository, template, mock(IntegrationEventLogService.class), new KafkaOperationsProperties());
        var event = new KafkaDeadLetter("topic", 0, 3, "room", null, new byte[] {1}, "Error"); event.requestReplay(1L);
        when(repository.findReplayBatch(any(), any(), any())).thenReturn(List.of(event));
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("down")));
        service.publishPending();
        assertThat(event.getStatus()).isEqualTo(Status.REPLAY_PENDING);
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getReplayedAt()).isNull();
        assertThat(event.getNextAttemptAt()).isNotNull();
    }
}
