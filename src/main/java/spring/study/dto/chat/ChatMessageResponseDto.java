package spring.study.dto.chat;

import lombok.Getter;
import spring.study.entity.ChatMessage;
import spring.study.entity.ChatRoom;
import spring.study.entity.Member;
import spring.study.entity.MessageType;

@Getter
public class ChatMessageResponseDto {
    private Long id;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;

    public ChatMessageResponseDto(ChatMessage entity) {
        this.message = entity.getMessage();
        this.type = entity.getType();
        this.member = entity.getMember();
        this.room = entity.getRoom();
    }

    @Override
    public String toString() {
        return "ChatMessageResponseDto{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", type=" + type +
                ", member=" + member +
                ", room=" + room +
                '}';
    }
}
