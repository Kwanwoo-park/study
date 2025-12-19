package spring.study.chat.dto;

import lombok.*;
import spring.study.chat.entity.MessageType;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageRequestDto {
    private String id;
    private String message;
    private MessageType type;
    private String email;
    private String roomId;

    @Builder
    public ChatMessageRequestDto(String id, String message, MessageType type, String roomId, String email) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.email = email;
        this.roomId = roomId;
    }
}
