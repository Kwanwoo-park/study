package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

@Service
@RequiredArgsConstructor
public class KafkaQueueQueryService {
    private final KafkaOutboxEventRepository repository;

    @Transactional(readOnly = true)
    public Counts counts() {
        return new Counts(repository.countByDeadLetteredFalse(), repository.countByDeadLetteredTrue());
    }

    public record Counts(long pending, long failed) {}
}
