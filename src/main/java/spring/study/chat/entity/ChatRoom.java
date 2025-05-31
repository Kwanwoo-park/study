package spring.study.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Entity(name = "room")
public class ChatRoom implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "roomId", unique = true)
    private String roomId;

    @NotNull
    private String name;

    @NotNull
    private Long count;

    @JsonIgnore
    @OneToMany(mappedBy = "room", fetch = FetchType.EAGER)
    private List<ChatMessage> messages = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room", fetch = FetchType.EAGER)
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @Builder
    public ChatRoom(Long id, String roomId, String name, Long count) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
        this.count = count;
    }

    public void addChatRoomMembers(ChatRoomMember chatRoomMember) {
        chatRoomMember.addRoom(this);
    }

    public void addMessage(ChatMessage message) {
        message.addRoom(this);
    }

    public void addCount() {
        ++this.count;
    }

    public void subCount() {
        --this.count;
    }
}
