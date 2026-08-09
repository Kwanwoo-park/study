package spring.study.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.admin.entity.SystemIncident;

import java.util.List;

public interface SystemIncidentRepository extends JpaRepository<SystemIncident, Long> {
    List<SystemIncident> findTop50ByOrderByOccurredAtDesc();

    long countByAcknowledgedFalse();
}
