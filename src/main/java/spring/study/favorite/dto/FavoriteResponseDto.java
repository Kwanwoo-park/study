package spring.study.favorite.dto;

import lombok.Getter;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

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
