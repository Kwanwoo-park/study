package spring.study.dto.chat;

import lombok.Getter;
import spring.study.entity.ChatMessage;
import spring.study.entity.MessageType;

@Getter
public class ChatMessageResponseDto {
    private String roomId;
    private String sender;
    private String email;
    private String message;
    private MessageType type;

    public ChatMessageResponseDto(ChatMessage entity) {
        this.roomId = entity.getRoomId();
        this.sender = entity.getSender();
        this.message = entity.getMessage();
        this.type = entity.getType();
    }

    @Override
    public String toString() {
        return "ChatMessageResponseDto{" +
                "roomId='" + roomId +
                ", sender='" + sender +
                ", email='" + email +
                ", message='" + message +
                ", type=" + type +
                '}';
    }
}
