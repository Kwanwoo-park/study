package spring.study.board.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import spring.study.member.entity.Member;

@Entity
@Table(name = "board_bookmark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "board_id"}),
        indexes = @Index(name = "idx_board_bookmark_member_id", columnList = "member_id,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    public BoardBookmark(Member member, Board board) {
        this.member = member;
        this.board = board;
    }
}
