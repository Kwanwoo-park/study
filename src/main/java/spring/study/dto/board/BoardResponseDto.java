package spring.study.dto.board;

import lombok.Getter;
import spring.study.entity.board.Board;

import java.time.LocalDateTime;

@Getter
public class BoardResponseDto {
    private Long id;
    private String title;
    private String content;
    private int readCnt;
    private String registerId;
    private String registerEmail;
    private LocalDateTime registerTime;

    public BoardResponseDto(Board entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        content = entity.getContent();
        this.readCnt = entity.getReadCnt();
        this.registerId = entity.getRegisterId();
        this.registerEmail = entity.getRegisterEmail();
        this.registerTime = entity.getRegisterTime();
    }

    @Override
    public String toString() {
        return "BoardListDto [id=" + id + ", title=" + title + ", content=" + content
                + ", readCnt=" + readCnt + ", registerId=" + registerId + ", registerEmail=" + registerEmail +
                ", registerTime=" + registerTime +"]";
    }
}
