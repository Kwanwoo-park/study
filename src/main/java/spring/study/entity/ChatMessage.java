package spring.study.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.BasetimeEntity;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "message")
public class ChatMessage extends BasetimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

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

    @Builder
    public ChatMessage(Long id, String message, MessageType type, Member member, ChatRoom room) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.member = member;
        this.room = room;
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
