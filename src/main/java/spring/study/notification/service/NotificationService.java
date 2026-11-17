package spring.study.notification.service;

import spring.study.notification.component.NotificationReadStateCoordinator;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.kafka.component.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.entity.NotificationPeriod;
import spring.study.notification.repository.NotificationRepository;
import spring.study.notification.dto.NotificationReadState;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.dto.NotificationHistoryResponse;
import spring.study.common.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MessageProducer producer;
    private final NotificationReadStateCoordinator readStateCoordinator;

    @Transactional
    public Notification createNotification(Member member, String message, Group group) {
        return createNotification(member, message, group, null);
    }

    @Transactional
    public Notification createNotification(Member member, String message, Group group, String url) {
        Notification notification = createOrUpdateNotification(member, message, group, url);

        producer.sendNotification(notification);
        return notification;
    }

    @Transactional
    public void createNotification(List<ChatRoomMember> list, Member member, Group group) {
        for (ChatRoomMember roomMember : list) {
            createNotification(roomMember.getMember(), member.getName() + "님이 메시지를 보냈습니다.", group, roomMember.getRoom().getRoomId());
        }
    }

    @Transactional
    public void updateRead(Notification notification) {
        updateRead(notification.getId());
    }

    @Transactional
    public int updateAllRead(Member member, LocalDateTime readBefore) {
        int updated = notificationRepository.updateAll(member.getId(), Status.READ, readBefore);

        readStateCoordinator.afterCommit(new NotificationReadState(member.getId(), null, null, null, readBefore));

        return updated;
    }

    @Transactional
    public void updateReadByGroupAndUrl(Member member, Group group, String url) {
        if (url == null || url.isBlank()) {
            return;
        }

        LocalDateTime readBefore = LocalDateTime.now();
        notificationRepository.updateReadStatusByMemberAndGroupAndUrl(member, group, url, Status.UNREAD, Status.READ, readBefore);
        readStateCoordinator.afterCommit(new NotificationReadState(member.getId(), null, group.name(), url, readBefore));
    }

    @Transactional
    public void closeRealtimeNotification(Member member, Group group, String url) {
        if (url == null || url.isBlank())
            return;

        notificationRepository.findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(member, group, url, Status.UNREAD).ifPresent(notification -> {
            markAsRead(notification);

            producer.sendNotification(notification);
        });
    }

    @Transactional
    public void updateRead(Long id) {
        Notification notification = notificationRepository.findForReadById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 알림입니다"
        ));

        markAsRead(notification);
    }

    private void markAsRead(Notification notification) {
        coordinateRead(notification);
        notificationRepository.updateReadStatus(notification.getId(), Status.READ);
        notification.changeToRead();
    }

    private void coordinateRead(Notification notification) {
        // Capture scalars inside the transaction; never retain a lazy entity in the callback.
        LocalDateTime readBefore = LocalDateTime.now();
        LocalDateTime revision = notification.getUpdateTime() == null ? notification.getRegisterTime() : notification.getUpdateTime();

        if (revision != null && revision.isAfter(readBefore))
            readBefore = revision;

        readStateCoordinator.afterCommit(new NotificationReadState(notification.getMember().getId(), notification.getId(), null, null, readBefore));
    }

    @Transactional(readOnly = true)
    public NotificationHistoryResponse findHistory(Member member, Group group, NotificationPeriod period,
                                                   LocalDateTime asOf, LocalDateTime beforeTime, Long beforeId) {
        if ((beforeTime == null) != (beforeId == null))
            throw new IllegalArgumentException("알림 조회 커서를 확인해주세요");

        if (asOf == null)
            asOf = LocalDateTime.now();

        List<Notification> notifications = notificationRepository.findHistory(member, group, period.since(asOf), asOf,
                beforeTime, beforeId, PageRequest.of(0, 31));
        boolean hasMore = notifications.size() > 30;
        List<NotificationRealtimeEvent> list = notifications.stream().limit(30).map(NotificationRealtimeEvent::from).toList();

        if (!list.isEmpty()) {
            NotificationRealtimeEvent last = list.get(list.size() - 1);
            beforeTime = last.updateTime();
            beforeId = last.id();
        }

        return new NotificationHistoryResponse(10L, list, hasMore, asOf, beforeTime, beforeId,
                notificationRepository.countUnreadByGroup(member, Status.UNREAD));
    }

    @Transactional
    public void delete(Long id, Member member) {
        Notification notification = notificationRepository.findForReadById(id)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다"));

        if (!notification.getMember().getId().equals(member.getId()))
            throw new AccessDeniedException("본인의 알림만 삭제할 수 있습니다");

        coordinateRead(notification);
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public Long countUnReadNotification(Member member) {
        return notificationRepository.countByMemberAndReadStatus(member, Status.UNREAD);
    }

    @Transactional(readOnly = true)
    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다"));
    }

    @Transactional(readOnly = true)
    public Notification findForDelivery(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByMember(Member member) {
        return findByMember(member, 0, 100);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByMember(Member member, int page, int size) {
        return notificationRepository.findByMemberOrderByUpdateTimeDesc(member, PageRequest.of(page, Math.min(Math.max(size, 1), 100)));
    }

    @Transactional(readOnly = true)
    public List<Notification> findByMemberAndGroup(Member member, Group group) {
        return notificationRepository.findByMemberAndNotiGroupOrderByUpdateTimeDesc(member, group, PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public long countByMember(Member member) {
        return notificationRepository.countByMember(member);
    }

    @Transactional(readOnly = true)
    public long countByMemberAndGroup(Member member, Group group) {
        return notificationRepository.countByMemberAndNotiGroup(member, group);
    }

    @Transactional(readOnly = true)
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

        return notificationRepository.findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(member, group, url, Status.UNREAD).orElse(null);
    }
}
