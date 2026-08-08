package spring.study.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;
import org.springframework.data.domain.Persistable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "message")
public class ChatMessage extends BasetimeEntity implements Serializable, Persistable<String> {
    @Serial
    private static final long serialVersionUID = 10L;
    public static final String CENSORED_MESSAGE = "<부적절한 내용이 포함되어 검열되었습니다>";

    @Id
    @Column(name = "message_id")
    private String id;

    @NotNull
    private String message;

    @NotNull
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'ACTIVE'")
    private ChatMessageStatus status = ChatMessageStatus.ACTIVE;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean edited;

    @Column(name = "edited_by_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean editedByAdmin;

    @Column(name = "deleted_by_admin", nullable = false, columnDefinition = "boolean default false")
    private boolean deletedByAdmin;

    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JoinColumn(name = "room_id")
    @ManyToOne
    private ChatRoom room;

    @Transient
    @JsonIgnore
    private boolean newEntity = true;

    @Builder
    public ChatMessage(String id, String message, MessageType type, Member member, ChatRoom room,
                       LocalDateTime registerTime) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.status = ChatMessageStatus.ACTIVE;
        this.member = member;
        this.room = room;
        changeRegisterTime(registerTime);
    }

    @Override
    @JsonIgnore
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
        if (status == null) {
            status = ChatMessageStatus.ACTIVE;
        }
    }

    public void addMember(Member member) {
        this.member = member;
        member.getMessages().add(this);
    }

    public void addRoom(ChatRoom room) {
        this.room = room;
        room.getMessages().add(this);
    }

    public void edit(String message) {
        edit(message, false);
    }

    public void edit(String message, boolean byAdmin) {
        this.message = message;
        this.edited = true;
        this.editedByAdmin = byAdmin;
    }

    public boolean isCensored() {
        return CENSORED_MESSAGE.equals(message);
    }

    public void deleteForAll() {
        deleteForAll(false);
    }

    public void deleteForAll(boolean byAdmin) {
        this.status = ChatMessageStatus.DELETED_FOR_ALL;
        this.deletedByAdmin = byAdmin;
    }
}
