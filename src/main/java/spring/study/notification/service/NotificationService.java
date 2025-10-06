package spring.study.notification.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.repository.NotificationRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MessageProducer producer;

    @Transactional
    public Notification createNotification(Member member, String message) {
        Notification notification = Notification.builder()
                .member(member)
                .message(message)
                .readStatus(Status.UNREAD)
                .build();

        producer.sendNotification(notification);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void updateRead(Notification notification) {
        notification.changeToRead();
    }

    @Transactional
    public void updateAllRead(List<Long> list) {
        for (Long id : list) {
            notificationRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                    "존재하지 않는 알림입니다"
            )).changeToRead();
        }
    }

    @Transactional
    public int updateAllRead(Member member) {
        return notificationRepository.updateAll(member.getId(), Status.READ);
    }

    @Transactional
    public void updateRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 알림입니다"
        ));

        notification.changeToRead();
    }

    @Transactional
    public void updateCheck(Notification notification) {
        notification.changeToCheck();
    }

    @Transactional
    public void updateCheck(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 알림입니다"
        ));

        notification.changeToCheck();
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
    }

    public List<Notification> findByMember(Member member) {
        return notificationRepository.findByMember(member);
    }

    public List<Notification> findUnReadNotification(Member member) {
        return notificationRepository.findByMemberAndReadStatus(member, Status.UNREAD);
    }

    public void deleteByMember(Member member) {
        notificationRepository.deleteByMember(member);
    }
}
