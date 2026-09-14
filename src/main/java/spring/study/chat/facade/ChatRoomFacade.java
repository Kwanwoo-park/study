package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatRoomDetailsResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.dto.MobileChatRoomResponse;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomDetailsService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatRoomFacade {
    private final ChatViewFacade chatViewFacade;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final ChatRoomDetailsService chatRoomDetailsService;

    public ResponseEntity<?> rooms(Member member) {
        List<ChatRoom> rooms = chatViewFacade.chatList(member);
        Map<String, List<Member>> participants = chatRoomMemberService.findMember(rooms, member);
        Map<String, Long> unreadCounts = chatViewFacade.unreadCount(member, rooms);
        List<MobileChatRoomResponse> list = rooms.stream()
                .map(room -> MobileChatRoomResponse.from(room,
                        participants.getOrDefault(room.getRoomId(), List.of()),
                        unreadCounts.getOrDefault(room.getRoomId(), 0L)))
                .toList();

        return ResponseEntity.ok(Map.of("result", 1L, "list", list));
    }

    public ResponseEntity<ChatRoomDetailsResponse> details(String roomId, Member member) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(chatRoomDetailsService.details(roomId, member));
    }

    public ResponseEntity<ChatRoomImagesResponse> images(String roomId, Member member, Long cursor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(chatRoomDetailsService.images(roomId, member, cursor));
    }

    public ResponseEntity<?> active(String roomId, Member member) {
        chatPresenceService.active(roomId, member);
        notificationService.updateReadByGroupAndUrl(member, Group.CHAT, roomId);
        ChatRoom room = chatRoomService.find(roomId);

        if (room != null) {
            chatRoomMemberService.markRead(member, room);
        }

        return ResponseEntity.ok(Map.of("result", 1L));
    }

    public ResponseEntity<?> inactive(String roomId, Member member) {
        chatPresenceService.inactive(roomId, member);

        return ResponseEntity.ok(Map.of("result", 1L));
    }
}
