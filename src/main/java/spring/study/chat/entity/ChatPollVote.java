package spring.study.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import spring.study.member.entity.Member;

@Entity
@Table(name = "chat_poll_vote", uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatPollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private ChatPoll poll;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;

    public ChatPollVote(ChatPoll poll, Member member, int optionIndex) {
        this.poll = poll;
        this.member = member;
        this.optionIndex = optionIndex;
    }

    public void changeChoice(int optionIndex) {
        this.optionIndex = optionIndex;
    }
}
