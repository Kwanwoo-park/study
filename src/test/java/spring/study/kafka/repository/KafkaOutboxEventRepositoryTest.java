package spring.study.kafka.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.kafka.entity.KafkaOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class KafkaOutboxEventRepositoryTest {
    @Autowired
    private KafkaOutboxEventRepository repository;

    @Test
    void queryShouldReturnOnlyEventsWhoseRetryTimeHasArrived() {
        KafkaOutboxEvent event = repository.saveAndFlush(new KafkaOutboxEvent("topic", "room-1", KafkaOutboxEvent.PayloadType.CHAT_MESSAGE, "{}"));
        LocalDateTime now = LocalDateTime.now();

        List<KafkaOutboxEvent> readyEvents = repository.findNextBatchForUpdate(now.plusSeconds(1), PageRequest.of(0, 100));
        assertEquals(1, readyEvents.size());

        event.recordFailure("Kafka unavailable", now.plusMinutes(10));
        repository.flush();

        List<KafkaOutboxEvent> delayedEvents = repository.findNextBatchForUpdate(now.plusMinutes(1), PageRequest.of(0, 100));
        assertTrue(delayedEvents.isEmpty());
    }
}
