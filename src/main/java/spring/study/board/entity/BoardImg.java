package spring.study.board.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class BoardImg implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String imgSrc;

    @JsonIgnore
    @JoinColumn(name = "board_id")
    @ManyToOne
    private Board board;

    @Builder
    public BoardImg(Long id, String imgSrc, Board board) {
        this.id = id;
        this.imgSrc = imgSrc;
        this.board = board;
    }

    public void addBoard(Board board) {
        this.board = board;
        board.getImg().add(this);
    }
}
