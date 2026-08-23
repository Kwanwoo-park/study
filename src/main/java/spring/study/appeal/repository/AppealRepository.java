package spring.study.appeal.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.member.entity.Member;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    boolean existsByMemberAndStatus(Member member, AppealStatus status);

    @EntityGraph(attributePaths = {"member", "relatedSanction", "relatedSanction.report"})
    List<Appeal> findByMemberOrderByRegisterTimeDesc(Member member);

    @EntityGraph(attributePaths = {"member", "relatedSanction", "relatedSanction.report"})
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);

    @Transactional
    void deleteByMember(Member member);
}
