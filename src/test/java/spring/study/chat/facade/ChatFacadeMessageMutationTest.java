package spring.study.chat.facade;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageEventDto;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.*;
import spring.study.chat.service.*;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatFacadeMessageMutationTest {
    @Mock private ChatRoomService roomService;
    @Mock private ChatRoomMemberService roomMemberService;
    @Mock private ChatMessageService messageService;
    @Mock private ChatMessageHiddenService messageHiddenService;
    @Mock private ChatMessageImgService messageImgService;
    @Mock private MemberService memberService;
    @Mock private ForbiddenService forbiddenService;
    @Mock private ImageS3Service imageS3Service;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private ChatFacade chatFacade;

    @Test
    void deleteForMeShouldOnlyCreatePersonalHiddenState() {
        Member member = member(1L);
        ChatMessage message = message(member);
        when(messageService.findById(message.getId())).thenReturn(message);
        when(roomMemberService.exist(member, message.getRoom())).thenReturn(true);

        chatFacade.deleteMessage(message.getId(), ChatMessageDeleteScope.ME, member);

        verify(messageHiddenService).hideForMember(message, member);
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
