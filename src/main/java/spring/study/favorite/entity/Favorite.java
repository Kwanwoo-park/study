package spring.study.favorite.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity(name="favorite")
public class Favorite implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @JoinColumn(name = "member")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @JoinColumn(name = "board")
    @ManyToOne
    private Board board;

    @Builder
    public Favorite(Long id, Member member, Board board) {
        this.id = id;
        this.member = member;
        this.board = board;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getFavorites().add(this);
    }

    public void addBoard(Board board) {
        this.board = board;
        board.getFavorites().add(this);
    }
}
