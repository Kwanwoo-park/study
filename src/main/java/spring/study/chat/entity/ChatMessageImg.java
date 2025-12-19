package spring.study.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Setter
@Getter
@Entity
public class ChatMessageImg implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String imgSrc;

    @JsonIgnore
    @JoinColumn(name = "message_id")
    @ManyToOne
    private ChatMessage message;

    @Builder
    public ChatMessageImg(Long id, String imgSrc, ChatMessage message) {
        this.id = id;
        this.imgSrc = imgSrc;
        this.message = message;
    }

    public void addMessage(ChatMessage message) {
        this.message = message;
        message.getImg().add(this);
    }
}
