package spring.study.memo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import spring.study.memo.dto.MemoSummaryDto;
import spring.study.memo.entity.Memo;
import spring.study.member.entity.Member;

import java.util.Optional;
import java.time.LocalDateTime;
import spring.study.memo.entity.MemoColor;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Memo m set m.title = :title, m.content = :content, m.color = :color, m.pinned = :pinned, " +
            "m.version = :version, m.updateTime = :updatedAt where m.id = :id and m.owner.id = :ownerId and m.version = :previousVersion")
    int persistDraft(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("previousVersion") long previousVersion,
                     @Param("version") long version, @Param("title") String title, @Param("content") String content,
                     @Param("color") MemoColor color, @Param("pinned") boolean pinned, @Param("updatedAt") LocalDateTime updatedAt);

    // Search both fields without loading full memo bodies into the list response.
    @Query("select new spring.study.memo.dto.MemoSummaryDto(m.id, m.title, substring(m.content, 1, 160), " +
            "m.color, m.pinned, m.version, m.registerTime, m.updateTime) from Memo m " +
            "where m.owner = :owner and (:pinnedOnly = false or m.pinned = true) " +
            "and (:query = '' or lower(m.title) like :pattern escape '!' or lower(m.content) like :pattern escape '!')")
    @Transactional(readOnly = true)
    Page<MemoSummaryDto> search(@Param("owner") Member owner, @Param("query") String query,
                                    @Param("pattern") String pattern, @Param("pinnedOnly") boolean pinnedOnly,
                                    Pageable pageable);

    @Transactional(readOnly = true)
    Optional<Memo> findByIdAndOwner(Long id, Member owner);

    @Transactional(readOnly = true)
    Optional<Memo> findByCreationKeyAndOwner(String creationKey, Member owner);

    @Transactional(readOnly = true)
    long countByOwner(Member owner);

    @Transactional(readOnly = true)
    long countByOwnerAndPinnedTrue(Member owner);

    void deleteByOwner(Member owner);

    void deleteByIdAndOwner_Id(Long id, Long ownerId);
}
