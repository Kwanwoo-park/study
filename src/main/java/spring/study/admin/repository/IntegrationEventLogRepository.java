package spring.study.admin.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.admin.entity.IntegrationEventLog;
import spring.study.admin.entity.IntegrationEventLog.*;

import java.time.LocalDateTime;
import java.util.List;

public interface IntegrationEventLogRepository extends JpaRepository<IntegrationEventLog, Long> {
    @Transactional(readOnly = true)
    @Query("""
            select e from IntegrationEventLog e where e.occurredAt >= :since
            and (:broker is null or e.broker = :broker)
            and (:operation is null or e.operation = :operation)
            and (:outcome is null or e.outcome = :outcome)
            and (:beforeId is null or e.id < :beforeId) order by e.id desc
            """)
    List<IntegrationEventLog> search(@Param("since") LocalDateTime since, @Param("broker") Broker broker,
                                     @Param("operation") Operation operation, @Param("outcome") Outcome outcome,
                                     @Param("beforeId") Long beforeId, Pageable pageable);

    @Transactional(readOnly = true)
    @Query("select e.id from IntegrationEventLog e where e.occurredAt < :cutoff order by e.occurredAt, e.id")
    List<Long> findExpiredIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}
