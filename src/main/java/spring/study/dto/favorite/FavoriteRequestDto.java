package spring.study.dto.favorite;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;

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
