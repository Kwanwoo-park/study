package spring.study.chat.dto;

import spring.study.chat.entity.ChatRoom;
import spring.study.jwt.dto.MobileMemberResponse;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

public record MobileChatRoomResponse(String roomId, String name, String lastMessage, LocalDateTime lastChatTime, long unreadCount, List<MobileMemberResponse> participants) {
    public static MobileChatRoomResponse from(ChatRoom room, List<Member> participants, long unreadCount) {
        String displayName = participants.isEmpty()
                ? room.getName()
                : participants.stream().map(Member::getName).reduce((left, right) -> left + " · " + right).orElse(room.getName());
        return new MobileChatRoomResponse(
                room.getRoomId(),
                displayName,
                room.getLastMessage(),
                room.getLastChatTime(),
                unreadCount,
                participants.stream().map(MobileMemberResponse::new).toList()
        );
    }
}
