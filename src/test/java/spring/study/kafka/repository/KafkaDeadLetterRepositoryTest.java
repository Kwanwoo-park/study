package spring.study.kafka.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class KafkaDeadLetterRepositoryTest {
    @Autowired KafkaDeadLetterRepository repository;
    @Test
    void storesRawPayloadListsMetadataAndSelectsOnlyRequestedReplays() {
        byte[] raw = new byte[] {0, 1, -1, 20};
        KafkaDeadLetter event = repository.saveAndFlush(new KafkaDeadLetter("topic", 0, 7, "key", "outbox-1", raw, "BadPayload"));
        assertThat(repository.existsBySourceTopicAndSourcePartitionAndSourceOffset("topic", 0, 7)).isTrue();
        var page = repository.findRecent(Status.NEW, null, PageRequest.of(0, 31));
        assertThat(page).hasSize(1);
        assertThat(page.get(0).getPayloadAvailable()).isTrue();
        assertThat(repository.findReplayBatch(Status.REPLAY_PENDING, LocalDateTime.now(), PageRequest.of(0, 10))).isEmpty();
        event.requestReplay(1L); repository.flush();
        var pending = repository.findReplayBatch(Status.REPLAY_PENDING, LocalDateTime.now().plusSeconds(1), PageRequest.of(0, 10));
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getPayload()).containsExactly(raw);
        assertThatThrownBy(() -> event.requestReplay(1L)).isInstanceOf(IllegalStateException.class);
        event.replayed(); repository.flush();
        assertThat(repository.findRecent(Status.REPLAYED, event.getId(), PageRequest.of(0, 31))).isEmpty();
    }
}
