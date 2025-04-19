package spring.study.entity.comment.reply;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "reply")
public class Reply extends BasetimeEntity implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String reply;

    @JsonIgnore
    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @JsonIgnore
    @JoinColumn(name = "comment_id")
    @ManyToOne
    private Comment comment;

    @Builder
    public Reply(Long id, String reply, Member member, Comment comment) {
        this.id = id;
        this.reply = reply;
        this.member = member;
        this.comment = comment;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getReply().add(this);
    }

    public void addComment(Comment comment) {
        this.comment = comment;
        comment.getReply().add(this);
    }
}
