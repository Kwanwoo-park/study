package spring.study.kafka.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.dto.KafkaDeadLetterEntry;
import spring.study.kafka.service.KafkaDeadLetterService;
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
        var page = repository.findRecent(KafkaDeadLetterStatus.NEW, null, PageRequest.of(0, 31));
        assertThat(page).hasSize(1);
        assertThat(page.get(0).payloadAvailable()).isTrue();
        assertThat(KafkaDeadLetterEntry.from(page.get(0)).replayable()).isTrue();
        assertThat(repository.findForUpdateByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(KafkaDeadLetterStatus.REPLAY_PENDING, LocalDateTime.now(), PageRequest.of(0, 10))).isEmpty();
        event.requestReplay(1L); repository.flush();
        var pending = repository.findForUpdateByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(KafkaDeadLetterStatus.REPLAY_PENDING, LocalDateTime.now().plusSeconds(1), PageRequest.of(0, 10));
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getPayload()).containsExactly(raw);
        assertThatThrownBy(() -> event.requestReplay(1L)).isInstanceOf(IllegalStateException.class);
        event.replayed(); repository.flush();
        var replayed = repository.findRecent(KafkaDeadLetterStatus.REPLAYED, null, PageRequest.of(0, 31));
        assertThat(KafkaDeadLetterEntry.from(replayed.get(0)).replayable()).isFalse();
        assertThat(repository.findRecent(KafkaDeadLetterStatus.REPLAYED, event.getId(), PageRequest.of(0, 31))).isEmpty();
    }

    @Test
    void metadataListingKeepsReplayAvailabilityWithoutExposingRawEventData() {
        KafkaDeadLetter withPayload = repository.save(new KafkaDeadLetter("topic", 0, 1, "key", "event-1", new byte[] {1}, "Error"));
        KafkaDeadLetter withoutPayload = repository.saveAndFlush(new KafkaDeadLetter("topic", 0, 2, "key", "event-2", null, "Error"));

        var listing = new KafkaDeadLetterService(repository).list(null, null);

        assertThat(listing.entries()).extracting(KafkaDeadLetterEntry::id)
                .containsExactly(withoutPayload.getId(), withPayload.getId());
        assertThat(listing.entries()).extracting(KafkaDeadLetterEntry::replayable).containsExactly(false, true);
        assertThat(listing.nextBeforeId()).isNull();
        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(listing);
        assertThat(json.toString()).doesNotContain("\"payload\"", "\"eventKey\"", "\"eventId\"");
    }
}
