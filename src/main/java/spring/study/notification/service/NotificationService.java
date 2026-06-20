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

import java.time.LocalDateTime;
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
    public Notification createNotification(Member member, String message, Group group, String url) {
        Notification notification = findUpdatableNotification(member, group, url);

        if (notification == null) {
            notification = notificationRepository.save(Notification.builder()
                    .member(member)
                    .message(message)
                    .readStatus(Status.UNREAD)
                    .notiGroup(group)
                    .url(url)
                    .build());
        } else {
            notification.updateUnread(message, url);
        }

        producer.sendNotification(notification);
        return notification;
    }

    @Transactional
    public void createNotification(List<ChatRoomMember> list, Member member, Group group) {
        Notification notification;
        Member otherMember;

        for (ChatRoomMember roomMember : list) {
            otherMember = roomMember.getMember();

            notification = createOrUpdateNotification(
                    otherMember,
                    member.getName() + "님이 메시지를 보냈습니다.",
                    group,
                    roomMember.getRoom().getRoomId()
            );

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
    public int updateReadByGroupAndUrl(Member member, Group group, String url) {
        if (url == null || url.isBlank()) {
            return 0;
        }

        return notificationRepository.updateReadStatusByMemberAndGroupAndUrl(
                member,
                group,
                url,
                Status.UNREAD,
                Status.READ
        );
    }

    @Transactional
    public void updateRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 알림입니다"
        ));

        notification.changeToRead();
    }

    public Long countUnReadNotification(Member member) {
        return notificationRepository.countByMemberAndReadStatus(member, Status.UNREAD);
    }

    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다"));
    }

    public List<Notification> findByMember(Member member) {
        return notificationRepository.findByMember(member).stream()
                .sorted(Comparator.comparing(this::notificationSortTime).reversed())
                .toList();
    }

    public List<Notification> findUnReadNotification(Member member) {
        return notificationRepository.findByMemberAndReadStatus(member, Status.UNREAD);
    }

    public void deleteByMember(Member member) {
        notificationRepository.deleteByMember(member);
    }

    private Notification createOrUpdateNotification(Member member, String message, Group group, String url) {
        Notification notification = findUpdatableNotification(member, group, url);

        if (notification == null) {
            return notificationRepository.save(Notification.builder()
                    .member(member)
                    .message(message)
                    .readStatus(Status.UNREAD)
                    .notiGroup(group)
                    .url(url)
                    .build());
        }

        notification.updateUnread(message, url);
        return notification;
    }

    private Notification findUpdatableNotification(Member member, Group group, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        return notificationRepository.findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(
                member,
                group,
                url,
                Status.UNREAD
        ).orElse(null);
    }

    private LocalDateTime notificationSortTime(Notification notification) {
        return notification.getUpdateTime() == null ? notification.getRegisterTime() : notification.getUpdateTime();
    }
}
