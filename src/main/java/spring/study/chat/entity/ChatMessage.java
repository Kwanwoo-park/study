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

    @Id
    @Column(name = "message_id")
    private String id;

    @NotNull
    private String message;

    @NotNull
    private MessageType type;

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
    }

    public void addMember(Member member) {
        this.member = member;
        member.getMessages().add(this);
    }

    public void addRoom(ChatRoom room) {
        this.room = room;
        room.getMessages().add(this);
    }
}
