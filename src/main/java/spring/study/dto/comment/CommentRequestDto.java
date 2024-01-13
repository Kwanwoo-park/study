package spring.study.dto.comment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.comment.Comment;

@Getter
@Setter
@NoArgsConstructor
public class CommentRequestDto {
    private Long id;
    private String comment;
    private Long mid;
    private String mname;
    private Long bid;

    public Comment toEntity() {
        return Comment.builder()
                .comment(comment)
                .mid(mid)
                .mname(mname)
                .bid(bid)
                .build();
    }
}
