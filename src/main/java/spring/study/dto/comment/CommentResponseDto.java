package spring.study.dto.comment;

import lombok.Getter;
import spring.study.entity.comment.Comment;

@Getter
public class CommentResponseDto {
    private String comment;
    private Long mid;
    private String mname;
    private Long bid;

    public CommentResponseDto(Comment entity) {
        this.comment = entity.getComment();
        this.mid = entity.getMid();
        this.mname = entity.getMname();
        this.bid = entity.getBid();
    }

    @Override
    public String toString() {
        return "CommentResponseDto{" +
                "comment='" + comment +
                ", mid=" + mid +
                ", mname='" + mname +
                ", bid=" + bid +
                '}';
    }
}
