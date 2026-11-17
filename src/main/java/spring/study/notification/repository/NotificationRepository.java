package spring.study.notification.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Status;
import spring.study.notification.dto.NotificationGroupCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Transactional(readOnly = true)
    @Query("""
            select n from Notification n
            where n.member = :member and (:group is null or n.notiGroup = :group)
            and (:since is null or n.updateTime >= :since) and n.updateTime <= :asOf
            and (:beforeTime is null or n.updateTime < :beforeTime
                 or (n.updateTime = :beforeTime and n.id < :beforeId))
            order by n.updateTime desc, n.id desc
            """)
    List<Notification> findHistory(@Param("member") Member member, @Param("group") Group group,
                                   @Param("since") LocalDateTime since, @Param("asOf") LocalDateTime asOf,
                                   @Param("beforeTime") LocalDateTime beforeTime, @Param("beforeId") Long beforeId,
                                   Pageable pageable);

    @Transactional(readOnly = true)
    @Query("""
            select new spring.study.notification.dto.NotificationGroupCount(n.notiGroup, count(n))
            from Notification n where n.member = :member and n.readStatus = :status
            group by n.notiGroup
            """)
    List<NotificationGroupCount> countUnreadByGroup(@Param("member") Member member, @Param("status") Status status);

    // Reading must not update the event timestamp or move old notifications into today's history.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.readStatus = :status where n.id = :id")
    int updateReadStatus(@Param("id") Long id, @Param("status") Status status);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.readStatus = :status
            where n.member.id = :id and n.readStatus <> :status
            and n.updateTime <= :readBefore
            """)
    int updateAll(@Param("id") Long id, @Param("status") Status status, @Param("readBefore") LocalDateTime readBefore);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.readStatus = :status
            where n.member = :member and n.notiGroup = :group and n.url = :url
            and n.readStatus = :currentStatus
            and n.updateTime <= :readBefore
            """)
    int updateReadStatusByMemberAndGroupAndUrl(
            @Param("member") Member member,
            @Param("group") Group group,
            @Param("url") String url,
            @Param("currentStatus") Status currentStatus,
            @Param("status") Status status,
            @Param("readBefore") LocalDateTime readBefore
    );

    @Transactional(readOnly = true)
    List<Notification> findByMemberOrderByUpdateTimeDesc(Member member, Pageable pageable);

    @Transactional(readOnly = true)
    List<Notification> findByMemberAndNotiGroupOrderByUpdateTimeDesc(Member member, Group group, Pageable pageable);

    @Transactional(readOnly = true)
    List<Notification> findByMember(Member member);

    @Transactional(readOnly = true)
    List<Notification> findByMemberAndReadStatus(Member member, Status readStatus);

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notification> findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(Member member, Group notiGroup, String url, Status readStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notification> findForReadById(Long id);

    @Transactional(readOnly = true)
    long countByMemberAndReadStatus(Member member, Status readStatus);

    @Transactional(readOnly = true)
    long countByMember(Member member);

    @Transactional(readOnly = true)
    long countByMemberAndNotiGroup(Member member, Group group);

    @Transactional
    void deleteByMember(Member member);
}
