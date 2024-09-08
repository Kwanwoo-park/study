package spring.study.dto.chat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import spring.study.entity.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageRequestDto {
    private Long id;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;

    @Builder
    public ChatMessageRequestDto(String message, MessageType type, ChatRoom room, Member member) {
        this.message = message;
        this.type = type;
        this.member = member;
        this.room = room;
    }

    public ChatMessage toEntity() {
        return ChatMessage.builder()
                .message(message)
                .type(type)
                .member(member)
                .room(room)
                .build();
    }
}
