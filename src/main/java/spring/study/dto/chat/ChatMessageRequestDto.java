package spring.study.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.chat.ChatMessage;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private String roomId;
    private String sender;
    private String message;
    private ChatMessage.MessageType type;

    public ChatMessage toEntity() {
        return ChatMessage.builder()
                .roomId(roomId)
                .sender(sender)
                .message(message)
                .type(type)
                .build();
    }
}
