package spring.study.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import spring.study.entity.BasetimeEntity;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "comment")
public class Comment extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String comments;

    @JsonIgnore
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @JoinColumn(name = "board_id")
    @ManyToOne
    private Board board;

    @Builder
    public Comment(Long id, String comments, Member member, Board board) {
        this.id = id;
        this.comments = comments;
        this.member = member;
        this.board = board;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getComment().add(this);
    }

    public void addBoard(Board board) {
        this.board = board;
        board.getComment().add(this);
    }
}
