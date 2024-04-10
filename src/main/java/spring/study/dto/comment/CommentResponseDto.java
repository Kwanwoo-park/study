package spring.study.dto.comment;

import lombok.Getter;
import spring.study.entity.Comment;

@Getter
public class CommentResponseDto {
    private String comment;
    private Long mid;
    private String mname;
    private Long bid;
    private String email;

    public CommentResponseDto(Comment entity) {
        this.comment = entity.getComment();
        this.mid = entity.getMid();
        this.mname = entity.getMname();
        this.bid = entity.getBid();
        this.email = entity.getEmail();
    }

    @Override
    public String toString() {
        return "CommentResponseDto{" +
                "comment=" + comment +
                ", mid=" + mid +
                ", mname=" + mname +
                ", bid=" + bid +
                ", email=" + email +
                '}';
    }
}
