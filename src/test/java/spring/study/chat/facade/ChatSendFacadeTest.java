package spring.study.chat.facade;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.kafka.service.MessageProducer;
import spring.study.common.facade.CommonFacade;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatSendFacadeTest {

    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService roomMemberService = mock(ChatRoomMemberService.class);
    private final ChatMessageService messageService = mock(ChatMessageService.class);
    private final ChatMessageImgService chatMessageImgService = mock(ChatMessageImgService.class);
    private final ImageS3Service imageS3Service = mock(ImageS3Service.class);
    private final MemberService memberService = mock(MemberService.class);
    private final ChatPresenceService chatPresenceService = mock(ChatPresenceService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final MessageProducer producer = mock(MessageProducer.class);
    private final ChatFacade chatFacade = mock(ChatFacade.class);
    private final ChatSendFacade chatSendFacade = new ChatSendFacade(
            roomService,
            roomMemberService,
            messageService,
            chatMessageImgService,
            imageS3Service,
            memberService,
            chatPresenceService,
            notificationService,
            producer,
            chatFacade,
            new CommonFacade()
    );

    @Test
    void httpSendRejectsNonparticipantsBeforeModerationOrPublishing() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().roomId("room").build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().roomId("room").message("hello").build();
        when(roomService.find("room")).thenReturn(room);

        var response = chatSendFacade.sendMessage(dto, sender, mock(HttpServletResponse.class));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        verifyNoInteractions(chatFacade, producer, notificationService);
    }

    @Test
    void httpSendPreservesModerationFailureAndDoesNotPublish() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().roomId("room").build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().roomId("room").message("blocked").build();
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        ResponseEntity<?> failure = ResponseEntity.status(403).build();
        when(roomService.find("room")).thenReturn(room);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        doReturn(failure).when(chatFacade).messageCheck("blocked", sender, servletResponse);

        assertThat(chatSendFacade.sendMessage(dto, sender, servletResponse)).isSameAs(failure);
        verifyNoInteractions(producer, notificationService);
    }

    @Test
    void httpSendReplacesClientSuppliedIdentityWithAuthenticatedSender() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().roomId("room").build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().roomId("room").message("hello")
                .type(MessageType.TALK).email("forged@test.com").build();
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        when(roomService.find("room")).thenReturn(room);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.findMember(room, sender)).thenReturn(List.of());
        doReturn(ResponseEntity.ok().build()).when(chatFacade).messageCheck("hello", sender, servletResponse);

        assertThat(chatSendFacade.sendMessage(dto, sender, servletResponse).getStatusCode().value()).isEqualTo(200);
        assertThat(dto.getEmail()).isEqualTo(sender.getEmail());
        verify(producer).sendMessage(dto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void messageSendShouldNotNotifyMembersActiveInSameRoom() {
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .roomId("room-1")
                .name("test")
                .count(3L)
                .build();
        Member sender = createMember(1L, "sender@test.com");
        Member activeMember = createMember(2L, "active@test.com");
        Member inactiveMember = createMember(3L, "inactive@test.com");
        ChatRoomMember activeRoomMember = ChatRoomMember.builder()
                .room(room)
                .member(activeMember)
                .build();
        ChatRoomMember inactiveRoomMember = ChatRoomMember.builder()
                .room(room)
                .member(inactiveMember)
                .build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder()
                .type(MessageType.TALK)
                .roomId(room.getRoomId())
                .email(sender.getEmail())
                .message("hello")
                .build();

        when(roomService.find(room.getRoomId())).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.findMember(room, sender)).thenReturn(List.of(activeRoomMember, inactiveRoomMember));
        when(chatPresenceService.isActive(room.getRoomId(), activeMember)).thenReturn(true);
        when(chatPresenceService.isActive(room.getRoomId(), inactiveMember)).thenReturn(false);

        chatSendFacade.messageSend(dto);

        ArgumentCaptor<List<ChatRoomMember>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).createNotification(captor.capture(), eq(sender), eq(Group.CHAT));
        assertThat(captor.getValue()).containsExactly(inactiveRoomMember);
        ArgumentCaptor<ChatMessageRequestDto> messageCaptor = ArgumentCaptor.forClass(ChatMessageRequestDto.class);
        verify(producer).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRoom()).isNull();
        assertThat(messageCaptor.getValue().getMember()).isNull();
        assertThat(messageCaptor.getValue().getRoomId()).isEqualTo(room.getRoomId());
        assertThat(messageCaptor.getValue().getEmail()).isEqualTo(sender.getEmail());
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
