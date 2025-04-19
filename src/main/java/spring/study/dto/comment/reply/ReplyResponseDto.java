package spring.study.dto.comment.reply;

import lombok.Getter;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;

@Getter
public class ReplyResponseDto {
    private Long id;
    private String reply;
    private Member member;

    public ReplyResponseDto(Reply entity) {
        this.id = entity.getId();
        this.reply = entity.getReply();
        this.member = entity.getMember();
    }

    @Override
    public String toString() {
        return "ReplyResponseDto{" +
                "id=" + id +
                ", reply='" + reply + '\'' +
                ", member=" + member +
                '}';
    }
}
