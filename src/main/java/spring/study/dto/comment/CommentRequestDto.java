package spring.study.dto.comment;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequestDto {
    private Long id;
    private String comments;
    private Member member;
    private Board board;

    @Builder
    public CommentRequestDto(String comments) {
        this.comments = comments;
    }

    public Comment toEntity() {
        return Comment.builder()
                .comments(comments)
                .member(member)
                .board(board)
                .build();
    }
}
