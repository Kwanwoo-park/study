package spring.study.entity;

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
    private Long count;

    @Builder
    public ChatRoom(Long id, String roomId, String name, Long count) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
        this.count = count;
    }
}
