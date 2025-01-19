package spring.study.entity.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity(name = "board")
public class Board extends BasetimeEntity implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;
    @Column(name = "title")
    @NotNull
    private String title;
    @Column(name = "content")
    @NotNull
    private String content;
    @Column(name = "read_cnt")
    private int readCnt;
    @JsonIgnore
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @OneToMany(mappedBy = "board", fetch = FetchType.EAGER)
    private List<Comment> comment = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "board", fetch = FetchType.EAGER)
    private List<BoardImg> img = new ArrayList<>();

    @Builder
    public Board(Long id, String title, String content, int readCnt, Member member) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.readCnt = readCnt;
        this.member = member;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getBoard().add(this);
    }

    public void addComment(Comment comment) {
        comment.addBoard(this);
    }

    public void addImg(BoardImg boardImg) {
        boardImg.addBoard(this);
    }

    public void changeReadCnt() {
        this.readCnt++;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeTitle(String title) {
        this.title = title;
    }

    public void removeComment(Comment cmt) {
        for (Comment c : comment) {
            if (c.getId().equals(cmt.getId())) {
                comment.remove(c);
                break;
            }
        }
    }
}
