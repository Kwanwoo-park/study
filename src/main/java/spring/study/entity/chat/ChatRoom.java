package spring.study.entity.chat;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Entity(name = "room")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomId;
    private String name;

    @Builder
    public ChatRoom(Long id, String roomId, String name) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
    }
}
