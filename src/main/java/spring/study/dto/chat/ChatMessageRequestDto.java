package spring.study.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.ChatMessage;
import spring.study.entity.ChatRoom;
import spring.study.entity.Member;
import spring.study.entity.MessageType;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private Long id;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;

    public ChatMessageRequestDto(String message, MessageType type) {
        this.message = message;
        this.type = type;
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
