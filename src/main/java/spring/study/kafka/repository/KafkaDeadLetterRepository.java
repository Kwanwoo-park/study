package spring.study.kafka.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.dto.KafkaDeadLetterMetadataDto;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetterStatus;
import java.time.LocalDateTime;
import java.util.List;

public interface KafkaDeadLetterRepository extends JpaRepository<KafkaDeadLetter, Long> {
    @Transactional(readOnly = true)
    @Query("""
            select new spring.study.kafka.dto.KafkaDeadLetterMetadataDto(
                e.id, e.sourceTopic, e.sourcePartition, e.sourceOffset, e.status, e.errorType,
                e.createdAt, e.replayedAt, e.nextAttemptAt, e.attemptCount,
                case when e.payload is null then false else true end
            )
            from KafkaDeadLetter e
            where (:status is null or e.status = :status)
              and (:before is null or e.id < :before)
            order by e.id desc
            """)
    List<KafkaDeadLetterMetadataDto> findRecent(@Param("status") KafkaDeadLetterStatus status, @Param("before") Long before, Pageable page);

    @Modifying
    @Query("delete from KafkaDeadLetter e where e.status = :status and e.replayedAt < :cutoff")
    void deleteReplayedBefore(@Param("status") KafkaDeadLetterStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Transactional(readOnly = true)
    boolean existsBySourceTopicAndSourcePartitionAndSourceOffset(String topic, int partition, long offset);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<KafkaDeadLetter> findForUpdateByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(KafkaDeadLetterStatus status, LocalDateTime now, Pageable page);

    @Transactional(readOnly = true)
    long countByStatusIn(List<KafkaDeadLetterStatus> statuses);
}
