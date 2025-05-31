package spring.study.favorite.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteRequestDto {
    private Long id;
    private Member member;
    private Board board;

    @Builder
    public FavoriteRequestDto(Board board, Member member) {
        this.board = board;
        this.member = member;
    }

    public Favorite toEntity() {
        return Favorite.builder()
                .member(member)
                .board(board)
                .build();
    }
}
