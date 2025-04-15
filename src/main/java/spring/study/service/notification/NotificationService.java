package spring.study.service.notification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.repository.notification.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(Member member, String message) {
        return notificationRepository.save(Notification.builder()
                        .member(member)
                        .message(message)
                        .isRead(false)
                .build());
    }

    @Transactional
    public void updateRead(Notification notification) {
        notification.changeIsRead();
    }

    @Transactional
    public void updateRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 알림입니다."
        ));

        notification.changeIsRead();
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
    }

    public List<Notification> findByMember(Member member) {
        return notificationRepository.findByMember(member);
    }

    public List<Notification> findUnReadNotification(Member member) {
        return notificationRepository.findByMemberAndIsRead(member, false);
    }

    public void deleteByMember(Member member) {
        notificationRepository.deleteByMember(member);
    }
}
