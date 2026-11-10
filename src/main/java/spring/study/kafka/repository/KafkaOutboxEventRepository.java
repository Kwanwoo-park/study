package spring.study.kafka.repository;

import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.dto.OutboxCreatedAtProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KafkaOutboxEventRepository extends JpaRepository<KafkaOutboxEvent, Long> {
    // Repeat the non-dead-letter condition in both OR branches.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<KafkaOutboxEvent> findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(LocalDateTime now, Pageable pageable);

    @Transactional(readOnly = true)
    long countByDeadLetteredTrue();

    @Transactional(readOnly = true)
    long countByDeadLetteredFalse();

    @Transactional(readOnly = true)
    Optional<OutboxCreatedAtProjection> findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc();
}
