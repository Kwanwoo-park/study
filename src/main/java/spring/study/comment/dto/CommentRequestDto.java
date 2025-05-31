package spring.study.comment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;

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
