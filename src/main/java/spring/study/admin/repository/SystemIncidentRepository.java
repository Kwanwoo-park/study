package spring.study.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.admin.entity.SystemIncident;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemIncidentRepository extends JpaRepository<SystemIncident, Long> {
    List<SystemIncident> findTop50ByOrderByOccurredAtDesc();

    long countByAcknowledgedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SystemIncident incident " +
            "set incident.acknowledged = true, incident.acknowledgedAt = :acknowledgedAt " +
            "where incident.acknowledged = false")
    int acknowledgeAll(@Param("acknowledgedAt") LocalDateTime acknowledgedAt);

    Optional<SystemIncident> findFirstByRequestMethodAndRequestPathAndRequestIpAndExceptionTypeAndErrorMessageAndAcknowledgedFalse(String requestMethod, String requestPath, String requestIp, String exceptionType, String errorMessage);
}
