package spring.study.kafka.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.kafka.entity.KafkaDeadLetter;
import spring.study.kafka.entity.KafkaDeadLetter.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KafkaDeadLetterRepository extends JpaRepository<KafkaDeadLetter, Long> {
    @Transactional(readOnly = true)
    boolean existsBySourceTopicAndSourcePartitionAndSourceOffset(String topic, int partition, long offset);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from KafkaDeadLetter e where e.id = :id")
    Optional<KafkaDeadLetter> lockById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from KafkaDeadLetter e where e.status = :status and e.nextAttemptAt <= :now order by e.id")
    List<KafkaDeadLetter> findReplayBatch(@Param("status") Status status, @Param("now") LocalDateTime now, Pageable page);
    @Transactional(readOnly = true)
    @Query("select e.id as id, e.sourceTopic as sourceTopic, e.sourcePartition as sourcePartition, e.sourceOffset as sourceOffset, " +
            "e.status as status, e.errorType as errorType, e.createdAt as createdAt, e.replayedAt as replayedAt, e.nextAttemptAt as nextAttemptAt, " +
            "e.attemptCount as attemptCount, case when e.payload is null then false else true end as payloadAvailable " +
            "from KafkaDeadLetter e where (:status is null or e.status = :status) and (:before is null or e.id < :before) order by e.id desc")
    List<Metadata> findRecent(@Param("status") Status status, @Param("before") Long before, Pageable page);
    @Transactional(readOnly = true)
    long countByStatusIn(List<Status> statuses);
    @Modifying
    @Query("delete from KafkaDeadLetter e where e.status = :status and e.replayedAt < :cutoff")
    int deleteReplayedBefore(@Param("status") Status status, @Param("cutoff") LocalDateTime cutoff);

    interface Metadata {
        Long getId(); String getSourceTopic(); int getSourcePartition(); long getSourceOffset();
        Status getStatus(); String getErrorType(); LocalDateTime getCreatedAt(); LocalDateTime getReplayedAt();
        LocalDateTime getNextAttemptAt(); int getAttemptCount(); boolean getPayloadAvailable();
    }
}
