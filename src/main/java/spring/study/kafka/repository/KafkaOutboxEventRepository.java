package spring.study.kafka.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.kafka.entity.KafkaOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface KafkaOutboxEventRepository extends JpaRepository<KafkaOutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from KafkaOutboxEvent event where event.deadLettered = false and (event.nextAttemptAt is null or event.nextAttemptAt <= :now) order by event.id")
    List<KafkaOutboxEvent> findNextBatchForUpdate(@Param("now") LocalDateTime now, Pageable pageable);

    long countByDeadLetteredTrue();
    long countByDeadLetteredFalse();
}
