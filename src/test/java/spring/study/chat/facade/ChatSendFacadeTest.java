package spring.study.chat.facade;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.controller.ChatWebSocketApiController;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.kafka.component.MessageProducer;
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
    private final ChatSendFacade chatSendFacade = new ChatSendFacade(
            roomService,
            roomMemberService,
            messageService,
            chatMessageImgService,
            imageS3Service,
            memberService,
            chatPresenceService,
            notificationService,
            producer
    );

    @Test
    void socketSendReplacesClientSuppliedIdentityWithAuthenticatedSender() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().roomId("room").build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().roomId("room").message("hello")
                .type(MessageType.TALK).email("forged@test.com").build();
        when(roomService.find("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.findMember(room, sender)).thenReturn(List.of());
        ChatWebSocketApiController controller = new ChatWebSocketApiController(chatSendFacade, mock(AudioCallFacade.class));
        assertThat(controller.sendMessage(dto, sender::getEmail).getStatusCode().value()).isEqualTo(200);

        assertThat(dto.getEmail()).isEqualTo(sender.getEmail());
        verify(producer).sendMessage(dto);
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
