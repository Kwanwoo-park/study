package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.repository.KafkaOutboxEventRepository;
import spring.study.kafka.dto.KafkaQueueCounts;

@Service
@RequiredArgsConstructor
public class KafkaQueueQueryService {
    private final KafkaOutboxEventRepository repository;

    @Transactional(readOnly = true)
    public KafkaQueueCounts counts() {
        return new KafkaQueueCounts(repository.countByDeadLetteredFalse(), repository.countByDeadLetteredTrue());
    }

}
