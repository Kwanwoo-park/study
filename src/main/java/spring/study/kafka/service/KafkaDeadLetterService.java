package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import spring.study.kafka.dto.KafkaDeadLetterEntry;
import spring.study.kafka.dto.KafkaDeadLetterListing;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class KafkaDeadLetterService {
    private final KafkaDeadLetterRepository repository;
    private final IntegrationEventLogService eventLogs;

    @Transactional
    public void archive(ConsumerRecord<String, byte[]> record) {
        String topic = KafkaTopics.sourceForDlt(record.topic());
        byte[] partitionHeader = header(record, KafkaHeaders.DLT_ORIGINAL_PARTITION);
        byte[] offsetHeader = header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);
        if (partitionHeader == null || partitionHeader.length != 4 || offsetHeader == null || offsetHeader.length != 8)
            throw new IllegalArgumentException("DLT source metadata missing");
        int partition = ByteBuffer.wrap(partitionHeader).getInt();
        long offset = ByteBuffer.wrap(offsetHeader).getLong();
        if (repository.existsBySourceTopicAndSourcePartitionAndSourceOffset(topic, partition, offset)) return;
        repository.save(new KafkaDeadLetter(topic, partition, offset, record.key(),
                textHeader(record, KafkaTopics.EVENT_ID_HEADER), record.value(), textHeader(record, KafkaHeaders.DLT_EXCEPTION_FQCN)));
    }

    @Transactional(readOnly = true)
    public KafkaDeadLetterListing list(KafkaDeadLetterStatus status, Long before) {
        if (before != null && before < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var rows = repository.findRecent(status, before, PageRequest.of(0, 31));
        List<KafkaDeadLetterEntry> entries = rows.stream().limit(30).map(KafkaDeadLetterEntry::from).toList();
        return new KafkaDeadLetterListing(entries, rows.size() > 30 ? entries.get(entries.size() - 1).id() : null);
    }

    @Transactional
    public void requestReplay(long id, Long administratorId) {
        KafkaDeadLetter entry = repository.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다."));
        try { entry.requestReplay(administratorId); }
        catch (IllegalStateException error) { throw new ResponseStatusException(HttpStatus.CONFLICT, error.getMessage()); }
        eventLogs.afterCommit(IntegrationEventLogRoute.kafka(entry.getSourceTopic()), IntegrationEventLogOperation.REPLAY, IntegrationEventLogOutcome.QUEUED, entry.getId(), 1, 0, null);
    }

    private static byte[] header(ConsumerRecord<?, ?> record, String name) {
        var header = record.headers().lastHeader(name); return header == null ? null : header.value();
    }
    private static String textHeader(ConsumerRecord<?, ?> record, String name) {
        byte[] value = header(record, name);
        if (value == null) return null;
        String text = new String(value, StandardCharsets.UTF_8); return text.substring(0, Math.min(255, text.length()));
    }
}
