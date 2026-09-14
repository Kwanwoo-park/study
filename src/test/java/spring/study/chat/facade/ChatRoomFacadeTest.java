package spring.study.chat.facade;

import org.junit.jupiter.api.Test;
import spring.study.chat.dto.MobileChatRoomResponse;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.*;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatRoomFacadeTest {
    private final ChatViewFacade viewFacade = mock(ChatViewFacade.class);
    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService memberService = mock(ChatRoomMemberService.class);
    private final ChatPresenceService presenceService = mock(ChatPresenceService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ChatRoomFacade facade = new ChatRoomFacade(viewFacade, roomService, memberService,
            presenceService, notificationService, mock(ChatRoomDetailsService.class));
    private final Member member = Member.builder().id(1L).email("me@example.test").name("me").build();

    @Test
    void roomListPreservesParticipantProjectionUnreadCountAndFallbacks() {
        ChatRoom first = ChatRoom.builder().id(1L).roomId("first").name("first room").build();
        ChatRoom second = ChatRoom.builder().id(2L).roomId("second").name("second room").build();
        Member participant = Member.builder().id(2L).email("other@example.test").name("other").build();
        List<ChatRoom> rooms = List.of(first, second);
        when(viewFacade.chatList(member)).thenReturn(rooms);
        when(memberService.findMember(rooms, member)).thenReturn(new HashMap<>(Map.of("first", List.of(participant))));
        when(viewFacade.unreadCount(member, rooms)).thenReturn(Map.of("first", 3L));

        var response = facade.rooms(member);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("result")).isEqualTo(1L);
        List<?> list = (List<?>) body.get("list");
        MobileChatRoomResponse firstResponse = (MobileChatRoomResponse) list.get(0);
        MobileChatRoomResponse secondResponse = (MobileChatRoomResponse) list.get(1);
        assertThat(firstResponse.name()).isEqualTo("other");
        assertThat(firstResponse.unreadCount()).isEqualTo(3L);
        assertThat(firstResponse.participants().get(0).id()).isEqualTo(2L);
        assertThat(secondResponse.name()).isEqualTo("second room");
        assertThat(secondResponse.unreadCount()).isZero();
        assertThat(secondResponse.participants()).isEmpty();
    }

    @Test
    void activeRoomPreservesPresenceNotificationAndLastReadOrdering() {
        ChatRoom room = ChatRoom.builder().roomId("room").build();
        when(roomService.find("room")).thenReturn(room);

        assertThat(facade.active("room", member).getBody()).isEqualTo(Map.of("result", 1L));

        var order = inOrder(presenceService, notificationService, roomService, memberService);
        order.verify(presenceService).active("room", member);
        order.verify(notificationService).updateReadByGroupAndUrl(member, Group.CHAT, "room");
        order.verify(roomService).find("room");
        order.verify(memberService).markRead(member, room);
    }

    @Test
    void missingRoomDoesNotAttemptToMarkMembershipRead() {
        facade.active("missing", member);

        verifyNoInteractions(memberService);
    }

    @Test
    void inactiveRoomDoesNotChangeNotificationOrReadState() {
        assertThat(facade.inactive("room", member).getBody()).isEqualTo(Map.of("result", 1L));

        verify(presenceService).inactive("room", member);
        verifyNoInteractions(notificationService, memberService, roomService);
    }
}
