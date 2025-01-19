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
    private String title;
    private String content;
    private Member member;

    @Builder
    public BoardRequestDto(Long id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public Board toEntity() {
        return Board.builder()
                .title(title)
                .content(content)
                .member(member)
                .build();
    }
}
