package spring.study.entity.comment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;
import spring.study.entity.board.Board;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "comment")
public class Comment extends BasetimeEntity implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String comments;

    @JsonIgnore
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @JoinColumn(name = "board_id")
    @ManyToOne
    private Board board;

    @JsonIgnore
    @OneToMany(mappedBy = "comment", fetch = FetchType.EAGER)
    private List<Reply> reply = new ArrayList<>();

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

    public void addReply(Reply reply) {
        reply.addComment(this);
    }

    public void changeComments(String comments) {
        this.comments = comments;
    }
}
