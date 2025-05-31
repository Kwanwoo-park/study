package spring.study.comment.dto.reply;

import lombok.Getter;
import spring.study.reply.entity.Reply;
import spring.study.member.entity.Member;

@Getter
public class ReplyResponseDto {
    private Long id;
    private String reply;
    private Member member;
    private Member commentMember;

    public ReplyResponseDto(Reply entity) {
        this.id = entity.getId();
        this.reply = entity.getReply();
        this.member = entity.getMember();
        this.commentMember = entity.getComment().getMember();
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
