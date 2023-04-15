package spring.study.dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.board.Board;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDto {
    private Long id;
    private String title;
    private String content;
    private String registerId;

    public Board toEntity() {
        return Board.builder()
                .title(title)
                .content(content)
                .registerId(registerId)
                .build();
    }
}
