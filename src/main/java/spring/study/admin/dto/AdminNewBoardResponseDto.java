package spring.study.admin.dto;

import lombok.Getter;
import spring.study.board.entity.Board;

@Getter
public class AdminNewBoardResponseDto {
    private final Long id;
    private final String memberName;

    public AdminNewBoardResponseDto(Board board) {
        this.id = board.getId();
        this.memberName = board.getMember().getName();
    }
}
