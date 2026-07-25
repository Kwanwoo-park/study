package spring.study.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class BoardRequestDto {
    private Long id;
    private String content;
    private Member member;
    private CommonVisibility visibility;

    @Builder
    public BoardRequestDto(Long id, String content, Member member, CommonVisibility visibility) {
        this.id = id;
        this.content = content;
        this.member = member;
        this.visibility = visibility;
    }

    public Board toEntity() {
        return Board.builder()
                .content(content)
                .member(member)
                .visibility(visibility)
                .build();
    }
}
