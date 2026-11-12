package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.dto.KafkaQueueCounts;
import spring.study.kafka.dto.OutboxCreatedAtProjection;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaOutboxEventService {
    private final KafkaOutboxEventRepository outboxEventRepository;

    @Transactional(readOnly = true)
    public List<KafkaOutboxEvent> find(LocalDateTime time, Pageable pageable) {
        return outboxEventRepository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(time, pageable);
    }

    @Transactional(readOnly = true)
    public LocalDateTime findFirst() {
        return outboxEventRepository.findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc().map(OutboxCreatedAtProjection::createdAt).orElse(null);
    }

    @Transactional(readOnly = true)
    public Long countByDeadLetteredTrue() {
        return outboxEventRepository.countByDeadLetteredTrue();
    }

    @Transactional(readOnly = true)
    public Long countByDeadLetteredFalse() {
        return outboxEventRepository.countByDeadLetteredFalse();
    }

    @Transactional(readOnly = true)
    public KafkaQueueCounts counts() {
        return new KafkaQueueCounts(outboxEventRepository.countByDeadLetteredFalse(), outboxEventRepository.countByDeadLetteredTrue());
    }

    public void delete(KafkaOutboxEvent event) {
        outboxEventRepository.delete(event);
    }
}
