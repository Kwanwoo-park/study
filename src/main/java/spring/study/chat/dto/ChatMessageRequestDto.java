package spring.study.chat.dto;

import lombok.*;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageRequestDto {
    private String id;
    private String message;
    private MessageType type;
    private String email;
    private String roomId;
    private ChatRoom room;
    private Member member;
    private LocalDateTime registerTime;
    private List<String> list;

    @Builder
    public ChatMessageRequestDto(String id, String message, MessageType type, String roomId, String email, ChatRoom room, LocalDateTime registerTime, Member member, List<String> list) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.email = email;
        this.roomId = roomId;
        this.room = room;
        this.member = member;
        this.registerTime = registerTime;
        this.list = list;
    }
}
