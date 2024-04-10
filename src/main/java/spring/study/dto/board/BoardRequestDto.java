package spring.study.dto.board;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Board;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDto {
    private Long id;
    private String title;
    private String content;
    private String registerId;
    private String registerEmail;

    public Board toEntity() {
        return Board.builder()
                .title(title)
                .content(content)
                .registerId(registerId)
                .registerEmail(registerEmail)
                .build();
    }
}
