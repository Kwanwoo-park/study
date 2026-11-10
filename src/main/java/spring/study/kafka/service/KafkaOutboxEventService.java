package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaOutboxEventService {
    private final KafkaOutboxEventRepository outboxEventRepository;

    public List<KafkaOutboxEvent> find(LocalDateTime time, Pageable pageable) {
        return outboxEventRepository.findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(time, pageable);
    }

    public void delete(KafkaOutboxEvent event) {
        outboxEventRepository.delete(event);
    }
}
