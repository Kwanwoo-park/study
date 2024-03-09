package spring.study.entity.chat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "room_member")
public class ChatMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomId;
    private String memName;
    private String email;

    @Builder
    public ChatMember(Long id, String roomId, String memName, String email) {
        this.id = id;
        this.roomId = roomId;
        this.memName = memName;
        this.email = email;
    }
}
