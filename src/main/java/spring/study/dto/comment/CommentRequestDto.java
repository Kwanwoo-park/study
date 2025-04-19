package spring.study.dto.comment;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.board.Board;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequestDto {
    private Long id;
    private String comments;
    private Member member;
    private Board board;

    @Builder
    public CommentRequestDto(Long id, String comments) {
        this.id = id;
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
