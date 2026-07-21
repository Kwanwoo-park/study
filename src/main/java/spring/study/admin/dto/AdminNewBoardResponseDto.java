package spring.study.admin.dto;

import lombok.Getter;
import spring.study.board.entity.Board;

@Getter
public class AdminNewBoardResponseDto {
    private final Long id;
    private final String memberName;
    private final String imageUrl;

    public AdminNewBoardResponseDto(Board board) {
        this.id = board.getId();
        this.memberName = board.getMember().getName();
        this.imageUrl = board.getImg().isEmpty() ? null : board.getImg().get(0).getImgSrc();
    }
}
