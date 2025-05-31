package spring.study.comment.dto;

import lombok.Getter;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;

@Getter
public class CommentResponseDto {
    private Long id;
    private String comments;
    private Member member;
    private Board board;

    public CommentResponseDto(Comment entity) {
        this.id = entity.getId();
        this.comments = entity.getComments();
        this.member = entity.getMember();
        this.board = entity.getBoard();
    }

    @Override
    public String toString() {
        return "CommentResponseDto{" +
                "comment=" + comments +
                ", member=" + member.getId() +
                ", board=" + board.getId() +
                '}';
    }
}
