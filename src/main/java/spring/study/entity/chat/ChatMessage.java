package spring.study.entity.chat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public enum MessageType {
        ENTER, TALK, QUIT
    }

    private MessageType type;
    private String roomId;
    private String sender;
    private String message;
    private String email;

    @Builder
    public ChatMessage(MessageType type, String roomId, String sender, String message, String email) {
        this.type = type;
        this.roomId = roomId;
        this.sender = sender;
        this.message = message;
        this.email = email;
    }
}
