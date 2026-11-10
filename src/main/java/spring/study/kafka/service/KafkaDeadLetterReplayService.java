package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import spring.study.kafka.repository.KafkaDeadLetterRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaDeadLetterReplayService {
    private final KafkaDeadLetterRepository repository;
    private final KafkaOperationsProperties properties;

    @Transactional
    public List<KafkaDeadLetter> findEntries() {
        return repository.findForUpdateByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(KafkaDeadLetterStatus.REPLAY_PENDING, LocalDateTime.now(), PageRequest.of(0, 10));
    }

    @Transactional
    public void cleanup() {
        repository.deleteReplayedBefore(KafkaDeadLetterStatus.REPLAYED, LocalDateTime.now().minusDays(properties.getReplayHistoryDays()));
    }
}
