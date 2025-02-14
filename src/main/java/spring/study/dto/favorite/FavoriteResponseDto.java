package spring.study.dto.favorite;

import lombok.Getter;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;

@Getter
public class FavoriteResponseDto {
    private Long id;
    private String comments;
    private Member member;
    private Board board;

    public FavoriteResponseDto(Favorite entity) {
        this.id = entity.getId();
        this.member = entity.getMember();
        this.board = entity.getBoard();
    }

    @Override
    public String toString() {
        return "FavoriteResponseDto{" +
                ", member=" + member.getId() +
                ", board=" + board.getId() +
                '}';
    }
}
