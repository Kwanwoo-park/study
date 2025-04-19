package spring.study.dto.comment.reply;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;

@Getter
@Setter
@NoArgsConstructor
public class ReplyRequestDto {
    private Long id;
    private String reply;
    private Member member;
    private Comment comment;

    @Builder
    public ReplyRequestDto(Long id, String reply) {
        this.id = id;
        this.reply = reply;
    }

    public Reply toEntity() {
        return Reply.builder()
                .reply(reply)
                .member(member)
                .comment(comment)
                .build();
    }
}
