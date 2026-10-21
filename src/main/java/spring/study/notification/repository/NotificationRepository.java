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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.readStatus = :status
            where n.member.id = :id
            and (coalesce(n.updateTime, n.registerTime) is null
                 or coalesce(n.updateTime, n.registerTime) <= :readBefore)
            """)
    int updateAll(@Param("id") Long id, @Param("status") Status status, @Param("readBefore") LocalDateTime readBefore);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.readStatus = :status
            where n.member = :member and n.notiGroup = :group and n.url = :url
            and n.readStatus = :currentStatus
            and (coalesce(n.updateTime, n.registerTime) is null
                 or coalesce(n.updateTime, n.registerTime) <= :readBefore)
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
    List<Notification> findByMember(Member member);

    @Query("select n from Notification n where n.member = :member order by coalesce(n.updateTime, n.registerTime) desc")
    @Transactional(readOnly = true)
    List<Notification> findRecentByMember(@Param("member") Member member, Pageable pageable);

    @Query("select n from Notification n where n.member = :member and n.notiGroup = :group order by coalesce(n.updateTime, n.registerTime) desc")
    @Transactional(readOnly = true)
    List<Notification> findRecentByMemberAndGroup(@Param("member") Member member, @Param("group") Group group, Pageable pageable);

    @Transactional(readOnly = true)
    List<Notification> findByMemberAndReadStatus(Member member, Status readStatus);

    @Transactional(readOnly = true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Notification> findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(
            Member member,
            Group notiGroup,
            String url,
            Status readStatus
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id")
    Optional<Notification> findForReadById(@Param("id") Long id);

    @Transactional(readOnly = true)
    List<Notification> findByReadStatusAndRegisterTimeBefore(Status readStatus, LocalDateTime registerTime);

    @Transactional(readOnly = true)
    long countByMemberAndReadStatus(Member member, Status readStatus);
    @Transactional(readOnly = true)
    long countByMember(Member member);
    @Transactional(readOnly = true)
    long countByMemberAndNotiGroup(Member member, Group group);

    @Transactional
    void deleteByRegisterTimeBefore(LocalDateTime registerTime);

    @Transactional
    void deleteByMember(Member member);
}
