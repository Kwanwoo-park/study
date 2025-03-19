package spring.study.entity.board;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.comment.Comment;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity(name = "board")
public class Board extends BasetimeEntity implements Serializable {
    private static final long serialVersionUID = 2L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column(name = "content")
    @NotNull
    private String content;

    @JsonIgnore
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @OneToMany(mappedBy = "board", fetch = FetchType.EAGER)
    private List<Favorite> favorites = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "board", fetch = FetchType.EAGER)
    private List<Comment> comment = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "board", fetch = FetchType.EAGER)
    private List<BoardImg> img = new ArrayList<>();

    @Builder
    public Board(Long id, String title, String content, int readCnt, Member member) {
        this.id = id;
        this.content = content;
        this.member = member;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getBoard().add(this);
    }

    public void addFavorite(Favorite favorite) {
        favorite.addBoard(this);
    }

    public void addComment(Comment comment) {
        comment.addBoard(this);
    }

    public void addImg(BoardImg boardImg) {
        boardImg.addBoard(this);
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void removeComment(Comment cmt) {
        for (Comment c : comment) {
            if (c.getId().equals(cmt.getId())) {
                comment.remove(c);
                break;
            }
        }
    }

    public void removeFavorite(Favorite favorite) {
        for (Favorite f : favorites) {
            if (f.getId().equals(favorite.getId())) {
                favorites.remove(f);
                break;
            }
        }
    }
}
