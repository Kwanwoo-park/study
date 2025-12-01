package spring.study.notification.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.repository.NotificationRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MessageProducer producer;

    @Transactional
    public Notification createNotification(Member member, String message, Group group) {
        Notification notification = notificationRepository.save(Notification.builder()
                .member(member)
                .message(message)
                .readStatus(Status.UNREAD)
                .notiGroup(group)
                .build());

        producer.sendNotification(notification);
        return notification;
    }

    @Transactional
    public void createNotification(List<ChatRoomMember> list, Member member, Group group) {
        Notification notification;
        Member otherMember;

        for (ChatRoomMember roomMember : list) {
            otherMember = roomMember.getMember();

            notification = notificationRepository.save(Notification.builder()
                    .member(otherMember)
                    .message(member.getName() + "님이 메시지를 보냈습니다.")
                    .readStatus(Status.UNREAD)
                    .notiGroup(group)
                    .build());

            notification.addMember(otherMember);

            producer.sendNotification(notification);
        }
    }

    @Transactional
    public void updateRead(Notification notification) {
        notification.changeToRead();
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

    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
    }

    public List<Notification> findByMember(Member member) {
        return notificationRepository.findByMember(member).stream().sorted(Comparator.comparing(Notification::getId).reversed()).toList();
    }

    public List<Notification> findUnReadNotification(Member member) {
        return notificationRepository.findByMemberAndReadStatus(member, Status.UNREAD);
    }

    public void deleteByMember(Member member) {
        notificationRepository.deleteByMember(member);
    }
}
