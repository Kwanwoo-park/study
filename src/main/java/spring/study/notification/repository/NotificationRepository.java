package spring.study.notification.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMember(Member member);

    List<Notification> findByMemberAndReadStatus(Member member, Status readStatus);

    List<Notification> findByReadStatusAndRegisterTimeBefore(Status readStatus, LocalDateTime registerTime);

    @Transactional
    void deleteByMember(Member member);
}
