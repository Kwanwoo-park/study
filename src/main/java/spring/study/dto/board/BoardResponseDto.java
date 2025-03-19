package spring.study.dto.board;

import lombok.Getter;
import spring.study.entity.board.Board;
import spring.study.entity.member.Member;

import java.time.LocalDateTime;

@Getter
public class BoardResponseDto {
    private Long id;
    private String content;
    private Member member;
    private LocalDateTime registerTime;

    public BoardResponseDto(Board entity) {
        this.id = entity.getId();
        this.content = entity.getContent();
        this.member = entity.getMember();
        this.registerTime = entity.getRegisterTime();
    }

    @Override
    public String toString() {
        return "BoardListDto [id=" + id + ", content=" + content
                + ", member=" + member.getId() + ", registerTime=" + registerTime +"]";
    }
}
