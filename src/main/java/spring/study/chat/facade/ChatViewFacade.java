package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatViewFacade {
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatMessageService chatMessageService;

    public List<ChatRoom> chatList(Member member) {
        List<ChatRoomMember> list = chatRoomMemberService.find(member);

        List<ChatRoom> roomList = list
                .stream()
                .map(ChatRoomMember::getRoom)
                .toList();

        return roomList.stream()
                .sorted(Comparator.comparing(
                        ChatRoom::getLastChatTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    public Map<String, Long> unreadCount(Member member, List<ChatRoom> rooms) {
        Map<String, Long> unreadCountMap = new HashMap<>();

        for (ChatRoom room : rooms) {
            ChatRoomMember roomMember = chatRoomMemberService.find(member, room);
            LocalDateTime lastReadAt = roomMember == null ? null : roomMember.getLastReadAt();

            long unreadCount = chatMessageService.countUnread(room, member, lastReadAt);

            unreadCountMap.put(room.getRoomId(), unreadCount);
        }

        return unreadCountMap;
    }
}
