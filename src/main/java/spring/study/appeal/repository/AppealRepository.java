package spring.study.appeal.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.member.entity.Member;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    @Modifying(flushAutomatically = true)
    @Query("update Appeal a set a.relatedSanction = null where a.relatedSanction.id in " +
            "(select s.id from MemberSanction s where s.member = :member or s.issuedBy = :member or s.report.reporter = :member)")
    void detachSanctions(@Param("member") Member member);

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
