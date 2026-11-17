package spring.study.chat.dto;

import spring.study.chat.entity.ChatRoom;
import spring.study.jwt.dto.MobileMemberResponse;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record MobileChatRoomResponse(String roomId, String name, String lastMessage, LocalDateTime lastChatTime, long unreadCount, List<MobileMemberResponse> participants) {
    public static MobileChatRoomResponse from(ChatRoom room, List<Member> participants, long unreadCount) {
        String displayName = room.getName();
        if (!participants.isEmpty())
            displayName = participants.stream().map(Member::getName).collect(Collectors.joining(" · "));

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
