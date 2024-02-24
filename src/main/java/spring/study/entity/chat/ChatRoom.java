package spring.study.entity.chat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
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
