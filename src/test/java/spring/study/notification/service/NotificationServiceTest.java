package spring.study.notification.service;

import org.junit.jupiter.api.Test;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.repository.NotificationRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final MessageProducer producer = mock(MessageProducer.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository, producer);

    @Test
    void createNotificationShouldUpdateExistingUnreadNotificationForSameTarget() {
        Member member = createMember(1L, "member@test.com");
        Notification existing = Notification.builder()
                .id(10L)
                .member(member)
                .message("old")
                .readStatus(Status.UNREAD)
                .notiGroup(Group.COMMENT)
                .url("100")
                .build();

        when(notificationRepository.findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(
                member,
                Group.COMMENT,
                "100",
                Status.UNREAD
        )).thenReturn(Optional.of(existing));

        Notification result = notificationService.createNotification(member, "new", Group.COMMENT, "100");

        assertThat(result).isEqualTo(existing);
        assertThat(existing.getMessage()).isEqualTo("new");
        assertThat(existing.getReadStatus()).isEqualTo(Status.UNREAD);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(producer).sendNotification(existing);
    }

    @Test
    void chatNotificationShouldUpdateExistingUnreadRoomNotificationAndStillSendEvent() {
        Member sender = createMember(1L, "sender@test.com");
        Member receiver = createMember(2L, "receiver@test.com");
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .roomId("room-1")
                .name("room")
                .count(2L)
                .build();
        ChatRoomMember roomMember = ChatRoomMember.builder()
                .room(room)
                .member(receiver)
                .build();
        Notification existing = Notification.builder()
                .id(20L)
                .member(receiver)
                .message("old")
                .readStatus(Status.UNREAD)
                .notiGroup(Group.CHAT)
                .url(room.getRoomId())
                .build();

        when(notificationRepository.findFirstByMemberAndNotiGroupAndUrlAndReadStatusOrderByIdDesc(
                receiver,
                Group.CHAT,
                room.getRoomId(),
                Status.UNREAD
        )).thenReturn(Optional.of(existing));

        notificationService.createNotification(List.of(roomMember), sender, Group.CHAT);

        assertThat(existing.getMessage()).isEqualTo(sender.getName() + "님이 메시지를 보냈습니다.");
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(producer).sendNotification(existing);
    }

    @Test
    void updateReadByGroupAndUrlShouldMarkMatchingUnreadNotificationAsRead() {
        Member member = createMember(1L, "member@test.com");

        when(notificationRepository.updateReadStatusByMemberAndGroupAndUrl(
                member,
                Group.CHAT,
                "room-1",
                Status.UNREAD,
                Status.READ
        )).thenReturn(1);

        int result = notificationService.updateReadByGroupAndUrl(member, Group.CHAT, "room-1");

        assertThat(result).isEqualTo(1);
        verify(notificationRepository).updateReadStatusByMemberAndGroupAndUrl(
                member,
                Group.CHAT,
                "room-1",
                Status.UNREAD,
                Status.READ
        );
    }

    private Member createMember(Long id, String email) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("password")
                .name("member" + id)
                .role(Role.USER)
                .profile("profile.png")
                .phone("010-0000-000" + id)
                .birth("2000-01-01")
                .build();
    }
}
