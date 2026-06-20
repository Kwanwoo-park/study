package spring.study.notification.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readStatus = :status where member.id = :id")
    int updateAll(@Param("id") Long id, @Param("status") Status status);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readStatus = :status where n.member = :member and n.notiGroup = :group and n.url = :url and n.readStatus = :currentStatus")
    int updateReadStatusByMemberAndGroupAndUrl(
            @Param("member") Member member,
            @Param("group") Group group,
            @Param("url") String url,
            @Param("currentStatus") Status currentStatus,
            @Param("status") Status status
    );

    List<Notification> findByMember(Member member);

    List<Notification> findByMemberAndReadStatus(Member member, Status readStatus);

    Optional<Notification> findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(
            Member member,
            Group notiGroup,
            String url,
            Status readStatus
    );

    List<Notification> findByReadStatusAndRegisterTimeBefore(Status readStatus, LocalDateTime registerTime);

    long countByMemberAndReadStatus(Member member, Status readStatus);

    @Transactional
    void deleteByRegisterTimeBefore(LocalDateTime registerTime);

    @Transactional
    void deleteByMember(Member member);
}
