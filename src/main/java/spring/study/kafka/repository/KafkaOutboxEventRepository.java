package spring.study.kafka.repository;

import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.dto.OutboxCreatedAtProjection;
import spring.study.kafka.dto.KafkaOutboxFailure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KafkaOutboxEventRepository extends JpaRepository<KafkaOutboxEvent, Long> {
    // Repeat the non-dead-letter condition in both OR branches.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<KafkaOutboxEvent> findForUpdateByDeadLetteredFalseAndNextAttemptAtIsNullOrDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByIdAsc(LocalDateTime now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<KafkaOutboxEvent> findForUpdateById(Long id);

    @Query("""
            select new spring.study.kafka.dto.KafkaOutboxFailure(event.id, event.topic, event.createdAt, event.attemptCount)
            from KafkaOutboxEvent event
            where event.deadLettered = true and (:beforeId is null or event.id < :beforeId)
            order by event.id desc
            """)
    List<KafkaOutboxFailure> findFailures(@Param("beforeId") Long beforeId, Pageable pageable);

    @Transactional(readOnly = true)
    long countByDeadLetteredTrue();

    @Transactional(readOnly = true)
    long countByDeadLetteredFalse();

    @Transactional(readOnly = true)
    Optional<OutboxCreatedAtProjection> findFirstByDeadLetteredFalseAndCreatedAtIsNotNullOrderByCreatedAtAsc();
}
