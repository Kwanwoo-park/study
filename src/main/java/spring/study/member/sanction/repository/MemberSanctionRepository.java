package spring.study.member.sanction.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberSanctionRepository extends JpaRepository<MemberSanction, Long> {
    @Transactional(readOnly = true)
    boolean existsByReportId(Long reportId);

    @EntityGraph(attributePaths = "report")
    @Transactional(readOnly = true)
    List<MemberSanction> findByMemberOrderByStartedAtDesc(Member member);

    @EntityGraph(attributePaths = "report")
    @Transactional(readOnly = true)
    Optional<MemberSanction> findByIdAndMember(Long id, Member member);
}
