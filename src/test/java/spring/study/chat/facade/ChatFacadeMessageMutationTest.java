package spring.study.chat.facade;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageEventDto;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.*;
import spring.study.chat.service.*;
import spring.study.common.service.ModerationService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatFacadeMessageMutationTest {
    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService roomMemberService = mock(ChatRoomMemberService.class);
    private final ChatMessageService messageService = mock(ChatMessageService.class);
    private final ChatMessageImgService messageImgService = mock(ChatMessageImgService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final ModerationService moderationService = mock(ModerationService.class);
    private final ImageS3Service imageS3Service = mock(ImageS3Service.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ChatFacade chatFacade = new ChatFacade(
            roomService,
            roomMemberService,
            messageService,
            messageImgService,
            memberService,
            moderationService,
            imageS3Service,
            messagingTemplate
    );

    @Test
    void updateShouldBroadcastChangedMessageWithoutReplacingOriginal() {
        Member owner = member(1L);
        ChatMessage message = message(owner);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ChatMessageRequestDto request = ChatMessageRequestDto.builder()
                .message("수정된 내용")
                .build();

        when(messageService.findById(message.getId())).thenReturn(message);
        when(roomMemberService.exist(owner, message.getRoom())).thenReturn(true);
        when(moderationService.validate("수정된 내용", owner, response)).thenReturn(0);
        message.edit("수정된 내용");
        when(messageService.edit(message.getId(), "수정된 내용", owner)).thenReturn(message);
        when(messageService.findLatestVisible(message.getRoom())).thenReturn(Optional.of(message));

        chatFacade.updateMessage(message.getId(), request, owner, response);

        verify(messagingTemplate).convertAndSend(
                eq("/sub/chat/room/room-1"),
                any(ChatMessageEventDto.class)
        );
        verify(messageImgService, never()).deleteMessage(any());
        verify(imageS3Service, never()).deleteImg(any());
    }

    @Test
    void deleteForMeShouldOnlyCreatePersonalHiddenState() {
        Member member = member(1L);
        ChatMessage message = message(member);
        when(messageService.findById(message.getId())).thenReturn(message);
        when(roomMemberService.exist(member, message.getRoom())).thenReturn(true);

        chatFacade.deleteMessage(message.getId(), ChatMessageDeleteScope.ME, member);

        verify(messageService).hideForMember(message.getId(), member);
        verify(messageService, never()).deleteForAll(any(), any());
        verifyNoInteractions(messagingTemplate);
        verify(messageImgService, never()).deleteMessage(any());
        verify(imageS3Service, never()).deleteImg(any());
    }

    @Test
    void deleteForAllShouldBroadcastRemovalAndPreserveStoredImageData() {
        Member owner = member(1L);
        ChatMessage message = message(owner);
        when(messageService.findById(message.getId())).thenReturn(message);
        when(roomMemberService.exist(owner, message.getRoom())).thenReturn(true);
        message.deleteForAll();
        when(messageService.deleteForAll(message.getId(), owner)).thenReturn(message);
        when(messageService.findLatestVisible(message.getRoom())).thenReturn(Optional.empty());

        chatFacade.deleteMessage(message.getId(), ChatMessageDeleteScope.ALL, owner);

        verify(messagingTemplate).convertAndSend(
                eq("/sub/chat/room/room-1"),
                any(ChatMessageEventDto.class)
        );
        verify(messageImgService, never()).deleteMessage(any());
        verify(imageS3Service, never()).deleteImg(any());
    }

    private ChatMessage message(Member owner) {
        return ChatMessage.builder()
                .id("message-1")
                .message("원본")
                .type(MessageType.TALK)
                .member(owner)
                .room(ChatRoom.builder()
                        .id(1L)
                        .roomId("room-1")
                        .name("room")
                        .count(2L)
                        .build())
                .registerTime(LocalDateTime.now())
                .build();
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .email("member" + id + "@test.com")
                .role(Role.USER)
                .build();
    }
}
