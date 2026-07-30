package spring.study.chat.dto;

import lombok.Getter;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.chat.entity.MessageType;
import spring.study.chat.entity.ChatMessageStatus;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponseDto {
    private String id;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;
    private ChatMessageStatus status;
    private boolean edited;

    public ChatMessageResponseDto(ChatMessage entity) {
        this.id = entity.getId();
        this.message = entity.getStatus() == ChatMessageStatus.DELETED_FOR_ALL
                ? "삭제된 메시지입니다."
                : entity.getMessage();
        this.type = entity.getType();
        this.member = entity.getMember();
        this.room = entity.getRoom();
        this.registerTime = entity.getRegisterTime();
        this.updateTime = entity.getUpdateTime();
        this.status = entity.getStatus();
        this.edited = entity.isEdited();
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
