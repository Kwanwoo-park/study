package spring.study.dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Board;
import spring.study.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDto {
    private Long id;
    private String title;
    private String content;
    private Member member;

    public Board toEntity() {
        return Board.builder()
                .title(title)
                .content(content)
                .build();
    }
}
