package spring.study.comment.dto.reply;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.comment.entity.Comment;
import spring.study.reply.entity.Reply;
import spring.study.member.entity.Member;

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
