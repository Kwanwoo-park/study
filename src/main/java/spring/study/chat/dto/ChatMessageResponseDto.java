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
    private String replyToId;
    private String message;
    private MessageType type;
    private Member member;
    private ChatRoom room;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;
    private ChatMessageStatus status;
    private boolean edited;
    private boolean censored;
    private boolean editedByAdmin;
    private boolean deletedByAdmin;

    public ChatMessageResponseDto(ChatMessage entity) {
        this.id = entity.getId();
        this.replyToId = entity.getReplyToId();
        this.message = entity.displayMessage();
        this.type = entity.getType();
        this.member = entity.getMember();
        this.room = entity.getRoom();
        this.registerTime = entity.getRegisterTime();
        this.updateTime = entity.getUpdateTime();
        this.status = entity.getStatus();
        this.edited = entity.isEdited();
        this.censored = entity.isCensored();
        this.editedByAdmin = entity.isEditedByAdmin();
        this.deletedByAdmin = entity.isDeletedByAdmin();
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
