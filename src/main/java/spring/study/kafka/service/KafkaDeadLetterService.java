package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.dto.KafkaDeadLetterEntry;
import spring.study.kafka.dto.KafkaDeadLetterListing;
import spring.study.kafka.dto.KafkaDeadLetterMetadataDto;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaDeadLetterService {
    private final KafkaDeadLetterRepository repository;
    private final KafkaOperationsProperties properties;

    @Transactional
    public void archive(ConsumerRecord<String, byte[]> record) {
        String topic = KafkaTopics.sourceForDlt(record.topic());
        byte[] partitionHeader = header(record, KafkaHeaders.DLT_ORIGINAL_PARTITION);
        byte[] offsetHeader = header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);

        if (partitionHeader == null || partitionHeader.length != 4 || offsetHeader == null || offsetHeader.length != 8)
            throw new IllegalArgumentException("DLT source metadata missing");

        int partition = ByteBuffer.wrap(partitionHeader).getInt();
        long offset = ByteBuffer.wrap(offsetHeader).getLong();

        if (repository.existsBySourceTopicAndSourcePartitionAndSourceOffset(topic, partition, offset))
            return;

        repository.save(KafkaDeadLetter.builder()
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .key(record.key())
                .eventId(textHeader(record, KafkaTopics.EVENT_ID_HEADER))
                .payload(record.value())
                .errorType(textHeader(record, KafkaHeaders.DLT_EXCEPTION_FQCN))
                .build());
    }

    @Transactional(readOnly = true)
    public KafkaDeadLetterListing list(KafkaDeadLetterStatus status, Long before) {
        if (before != null && before < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        List<KafkaDeadLetterMetadataDto> rows = repository.findRecent(status, before, PageRequest.of(0, 31));
        List<KafkaDeadLetterEntry> entries = rows.stream().limit(30).map(KafkaDeadLetterEntry::from).toList();

        return new KafkaDeadLetterListing(entries, rows.size() > 30 ? entries.get(entries.size() - 1).id() : null);
    }

    @Transactional
    public KafkaDeadLetter requestReplay(long id, Long administratorId) {
        KafkaDeadLetter entry = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."));

        try {
            entry.requestReplay(administratorId);
        } catch (IllegalStateException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, error.getMessage());
        }

        return entry;
    }

    @Transactional
    public List<KafkaDeadLetter> findEntries() {
        return repository.findForUpdateByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(KafkaDeadLetterStatus.REPLAY_PENDING, LocalDateTime.now(), PageRequest.of(0, 10));
    }

    @Transactional
    public void cleanup() {
        repository.deleteReplayedBefore(KafkaDeadLetterStatus.REPLAYED, LocalDateTime.now().minusDays(properties.getReplayHistoryDays()));
    }

    @Transactional(readOnly = true)
    public long countByStatusIn(List<KafkaDeadLetterStatus> list) {
        return repository.countByStatusIn(list);
    }

    private byte[] header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);

        return header == null ? null : header.value();
    }
    private String textHeader(ConsumerRecord<?, ?> record, String name) {
        byte[] value = header(record, name);

        if (value == null)
            return null;

        String text = new String(value, StandardCharsets.UTF_8);

        return text.substring(0, Math.min(255, text.length()));
    }
}
