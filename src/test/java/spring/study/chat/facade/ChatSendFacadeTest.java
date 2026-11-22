package spring.study.chat.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatImageUpload;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.controller.ChatWebSocketApiController;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatImageUploadService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.component.TransactionRunner;
import spring.study.common.component.ModerationPolicy;
import spring.study.kafka.component.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private final TransactionRunner transactions = mock(TransactionRunner.class);
    private final ModerationPolicy moderation = mock(ModerationPolicy.class);
    private final ChatImageUploadService uploads = mock(ChatImageUploadService.class);
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
            transactions,
            moderation,
            uploads
    );

    @BeforeEach
    void executeTransactionWork() {
        doAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get()).when(transactions).write(any());
    }

    @Test
    void socketSendReplacesClientSuppliedIdentityWithAuthenticatedSender() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room").build();
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().roomId("room").message("hello")
                .type(MessageType.TALK).email("forged@test.com").build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        when(roomMemberService.findMember(room, sender)).thenReturn(List.of());
        when(messageService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ChatWebSocketApiController controller = new ChatWebSocketApiController(chatSendFacade, mock(AudioCallFacade.class));
        assertThat(controller.sendMessage(dto, sender::getEmail).getStatusCode().value()).isEqualTo(200);

        assertThat(dto.getEmail()).isEqualTo(sender.getEmail());
        verify(producer).sendMessage(dto);
    }

    @Test
    void retriesOfTheSameTextDoNotSaveOrNotifyAgain() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room").build();
        String id = UUID.randomUUID().toString();
        ChatMessage saved = ChatMessage.builder().id(id).room(room).member(sender).type(MessageType.TALK).message("hello").build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        when(messageService.findExisting(id)).thenReturn(Optional.empty(), Optional.of(saved));
        when(messageService.save(any())).thenReturn(saved);

        for (int attempt = 0; attempt < 3; attempt++) {
            ChatMessageRequestDto message = ChatMessageRequestDto.builder().id(id).roomId("room")
                    .type(MessageType.TALK).email(sender.getEmail()).message("hello").build();
            assertThat(chatSendFacade.messageSend(message).getStatusCode().is2xxSuccessful()).isTrue();
        }
        verify(messageService).save(any());
        verify(producer).sendMessage(any());
        verify(notificationService).createNotification(List.of(), sender, Group.CHAT);
    }

    @Test
    void textRequiresRoomMembershipBeforeStoringOrQueueing() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room").build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);

        ChatMessageRequestDto message = ChatMessageRequestDto.builder().roomId("room")
                .type(MessageType.TALK).email(sender.getEmail()).message("hello").build();
        assertThat(chatSendFacade.messageSend(message).getStatusCode().value()).isEqualTo(403);
        verifyNoInteractions(messageService, producer, notificationService);
    }

    @Test
    void serverAppliesModerationBeforeStoringOrQueueingText() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room").build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        when(moderation.validate("reviewed message", sender)).thenReturn(3);

        var request = ChatMessageRequestDto.builder().type(MessageType.TALK).roomId("room")
                .email(sender.getEmail()).message("reviewed message").build();
        assertThat(chatSendFacade.messageSend(request).getStatusCode().value()).isEqualTo(403);
        verify(messageService, never()).save(any());
        verifyNoInteractions(producer, notificationService);
    }

    @Test
    void imageUsesStoredAttachmentsAndRetryDoesNotPublishAgain() {
        Member sender = createMember(1L, "sender@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room").build();
        String id = UUID.randomUUID().toString();
        var upload = new ChatImageUpload(id, sender.getId(), room.getId(), LocalDateTime.now().plusHours(1));
        assertThat(upload.markReady(LocalDateTime.now())).isTrue();
        var saved = ChatMessage.builder().id(id).room(room).member(sender).type(MessageType.IMAGE).message("사진을 보냈습니다").build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(sender.getEmail())).thenReturn(sender);
        when(roomMemberService.exist(sender, room)).thenReturn(true);
        when(uploads.lock(id)).thenReturn(upload);
        when(chatMessageImgService.findMessage(id)).thenReturn(List.of(
                ChatMessageImg.builder().messageId(id).imgSrc("https://images.example.test/stored.png").build()));
        when(messageService.findExisting(id)).thenReturn(Optional.empty(), Optional.of(saved));
        when(messageService.save(any())).thenReturn(saved);

        var request = ChatMessageRequestDto.builder().id(id).type(MessageType.IMAGE).roomId("room")
                .email(sender.getEmail()).list(List.of("https://images.example.test/unrelated.png")).build();
        assertThat(chatSendFacade.messageSend(request).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(chatSendFacade.messageSend(request).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(request.getList()).containsExactly("https://images.example.test/stored.png");
        verify(uploads).delete(upload);
        verify(messageService).save(any());
        verify(producer).sendMessage(request);
    }

    @Test
    void joiningALegacyDirectRoomRequiresAnInvitedMember() {
        Member invited = createMember(1L, "invited@test.com");
        Member outsider = createMember(2L, "outsider@test.com");
        ChatRoom room = ChatRoom.builder().id(1L).roomId("room")
                .name("invited@test.com second@test.com").count(1L).build();
        when(roomService.findForUpdate("room")).thenReturn(room);
        when(memberService.findMember(invited.getEmail())).thenReturn(invited);
        when(memberService.findMember(outsider.getEmail())).thenReturn(outsider);
        when(roomMemberService.exist(invited, room)).thenReturn(false, true);

        var denied = ChatMessageRequestDto.builder().type(MessageType.ENTER).roomId("room").email(outsider.getEmail()).build();
        var allowed = ChatMessageRequestDto.builder().type(MessageType.ENTER).roomId("room").email(invited.getEmail()).build();
        assertThat(chatSendFacade.messageSend(denied).getStatusCode().value()).isEqualTo(403);
        assertThat(chatSendFacade.messageSend(allowed).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(chatSendFacade.messageSend(allowed).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(room.getCount()).isEqualTo(2L);
        verify(roomMemberService).save(invited, room);
        verify(producer).sendMessage(allowed);
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
