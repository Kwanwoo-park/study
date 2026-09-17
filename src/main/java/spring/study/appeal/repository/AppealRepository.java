package spring.study.appeal.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.member.entity.Member;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    @Transactional(readOnly = true)
    boolean existsByMemberAndStatus(Member member, AppealStatus status);

    @EntityGraph(attributePaths = {"member", "relatedSanction", "relatedSanction.report"})
    @Transactional(readOnly = true)
    List<Appeal> findByMemberOrderByRegisterTimeDesc(Member member);

    @EntityGraph(attributePaths = {"member", "relatedSanction", "relatedSanction.report"})
    @Transactional(readOnly = true)
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);

    @Transactional
    void deleteByMember(Member member);
}
