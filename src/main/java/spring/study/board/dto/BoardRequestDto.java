package spring.study.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;

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
