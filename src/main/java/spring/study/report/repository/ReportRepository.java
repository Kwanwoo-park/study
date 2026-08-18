package spring.study.report.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import spring.study.member.entity.Member;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findByReporter(Member reporter, Pageable pageable);

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findByStatusIn(List<ReportStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = "reporter")
    List<Report> findByStatus(ReportStatus status, Sort sort);

    @Override
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "reporter")
    Optional<Report> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from report r join fetch r.reporter where r.id = :id")
    Optional<Report> findByIdForUpdate(@Param("id") Long id);

    boolean existsByReporterAndTargetTypeAndTargetIdAndStatusNot(
            Member reporter,
            ReportTargetType targetType,
            String targetId,
            ReportStatus status
    );

    @Transactional
    void deleteByReporter(Member reporter);
}
