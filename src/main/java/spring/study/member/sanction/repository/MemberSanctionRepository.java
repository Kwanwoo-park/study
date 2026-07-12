package spring.study.member.sanction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.member.sanction.entity.MemberSanction;

public interface MemberSanctionRepository extends JpaRepository<MemberSanction, Long> {
    boolean existsByReportId(Long reportId);
}
