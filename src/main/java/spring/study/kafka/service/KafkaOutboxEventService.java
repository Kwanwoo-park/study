package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.kafka.dto.KafkaQueueCounts;
import spring.study.kafka.dto.KafkaOutboxDispatchRequestedEvent;
import spring.study.kafka.dto.KafkaOutboxFailure;
import spring.study.kafka.dto.OutboxCreatedAtProjection;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaOutboxEventService {
    private static final int FAILURE_PAGE_SIZE = 20;

    private final KafkaOutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;

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

    @Transactional(readOnly = true)
    public KafkaOutboxFailure.Listing failures(Long beforeId) {
        List<KafkaOutboxFailure> result = outboxEventRepository.findFailures(beforeId, PageRequest.of(0, FAILURE_PAGE_SIZE + 1));
        List<KafkaOutboxFailure> entries = List.copyOf(result.subList(0, Math.min(result.size(), FAILURE_PAGE_SIZE)));
        Long next = result.size() > FAILURE_PAGE_SIZE ? entries.get(entries.size() - 1).id() : null;
        return new KafkaOutboxFailure.Listing(entries, next);
    }

    @Transactional
    public KafkaOutboxEvent requeueFailed(long id) {
        KafkaOutboxEvent event = outboxEventRepository.findForUpdateById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outbox 기록을 찾을 수 없습니다."));
        if (!event.isDeadLettered()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 발행 대기 중인 Outbox 기록입니다.");
        }

        event.requeue(LocalDateTime.now());
        eventPublisher.publishEvent(new KafkaOutboxDispatchRequestedEvent());
        return event;
    }

    public void delete(KafkaOutboxEvent event) {
        outboxEventRepository.delete(event);
    }
}
