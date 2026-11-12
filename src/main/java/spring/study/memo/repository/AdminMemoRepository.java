package spring.study.memo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.memo.dto.AdminMemoSummaryDto;
import spring.study.memo.entity.AdminMemo;
import spring.study.member.entity.Member;

import java.util.Optional;

public interface AdminMemoRepository extends JpaRepository<AdminMemo, Long> {
    // Search both fields without loading full memo bodies into the list response.
    @Query("select new spring.study.memo.dto.AdminMemoSummaryDto(m.id, m.title, substring(m.content, 1, 160), " +
            "m.color, m.pinned, m.version, m.registerTime, m.updateTime) from AdminMemo m " +
            "where m.owner = :owner and (:pinnedOnly = false or m.pinned = true) " +
            "and (:query = '' or lower(m.title) like :pattern escape '!' or lower(m.content) like :pattern escape '!')")
    @Transactional(readOnly = true)
    Page<AdminMemoSummaryDto> search(@Param("owner") Member owner, @Param("query") String query,
                                    @Param("pattern") String pattern, @Param("pinnedOnly") boolean pinnedOnly,
                                    Pageable pageable);

    @Transactional(readOnly = true)
    Optional<AdminMemo> findByIdAndOwner(Long id, Member owner);

    @Transactional(readOnly = true)
    long countByOwner(Member owner);

    @Transactional(readOnly = true)
    long countByOwnerAndPinnedTrue(Member owner);

    void deleteByOwner(Member owner);
}
