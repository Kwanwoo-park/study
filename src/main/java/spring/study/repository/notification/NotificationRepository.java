package spring.study.repository.notification;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMember(Member member);

    List<Notification> findByMemberAndIsRead(Member member, boolean isRead);

    List<Notification> findByIsReadAndRegisterTimeBefore(boolean isRead, LocalDateTime registerTime);

    @Transactional
    void deleteByMember(Member member);
}
