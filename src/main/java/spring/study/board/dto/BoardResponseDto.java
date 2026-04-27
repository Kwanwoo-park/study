package spring.study.board.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.comment.entity.Comment;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class BoardResponseDto {
    private Long id;
    private String content;
    private Member member;
    private List<BoardImg> img;
    private LocalDateTime registerTime;

    public BoardResponseDto(Board entity) {
        this.id = entity.getId();
        this.content = entity.getContent();
        this.member = entity.getMember();
        this.img = entity.getImg();
        this.registerTime = entity.getRegisterTime();
    }

    @Override
    public String toString() {
        return "BoardListDto [id=" + id + ", content=" + content
                + ", member=" + member.getId() + ", registerTime=" + registerTime +"]";
    }
}
