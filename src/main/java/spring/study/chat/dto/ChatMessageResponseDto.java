package spring.study.chat.dto;

import lombok.Getter;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.chat.entity.MessageType;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponseDto {
    private String id;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;
    private LocalDateTime registerTime;

    public ChatMessageResponseDto(ChatMessage entity) {
        this.id = entity.getId();
        this.message = entity.getMessage();
        this.type = entity.getType();
        this.member = entity.getMember();
        this.room = entity.getRoom();
        this.registerTime = entity.getRegisterTime();
    }

    @Override
    public String toString() {
        return "ChatMessageResponseDto{" +
                ", id='" + id + '\'' +
                ", message=" + message +
                ", type=" + type +
                ", member=" + member +
                ", room=" + room +
                ", registerTime=" + registerTime +
                '}';
    }
}
