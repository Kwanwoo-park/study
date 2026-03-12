package spring.study.comment.dto;

import lombok.Getter;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;

@Getter
public class CommentListResponseDto {
    private final Long id;
    private final String comments;
    private final Member member;
    private final int replyCount;

    public CommentListResponseDto(Comment entity) {
        this.id = entity.getId();
        this.comments = entity.getComments();
        this.member = entity.getMember();
        this.replyCount = entity.getReply().size();
    }
}
