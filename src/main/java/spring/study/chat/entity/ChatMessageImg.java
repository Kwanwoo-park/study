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

    @NotNull
    @Column(unique = true)
    private String message;

    @Builder
    public ChatMessageImg(Long id, String imgSrc, String message) {
        this.id = id;
        this.imgSrc = imgSrc;
        this.message = message;
    }
}
