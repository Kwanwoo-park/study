package spring.study.dto.board;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.board.Board;
import spring.study.entity.member.Member;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDto {
    private Long id;
    private String content;
    private Member member;

    @Builder
    public BoardRequestDto(Long id, String content, Member member) {
        this.id = id;
        this.content = content;
        this.member = member;
    }

    public Board toEntity() {
        return Board.builder()
                .content(content)
                .member(member)
                .build();
    }
}
