package spring.study.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity(name = "board")
public class Board extends BasetimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title")
    @NotNull
    private String title;
    @Column(name = "content")
    @NotNull
    private String content;
    @Column(name = "read_cnt")
    private int readCnt;
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Builder
    public Board(Long id, String title, String content, int readCnt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.readCnt = readCnt;
    }

    public void addMember(Member member) {
        member.getBoard().add(this);
        this.member = member;
    }
}
