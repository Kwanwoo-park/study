package spring.study.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_message_hidden",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_message_hidden_message_member",
                columnNames = {"message_id", "member_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageHidden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(nullable = false)
    private LocalDateTime hiddenAt;

    @Builder
    public ChatMessageHidden(ChatMessage message, Member member, LocalDateTime hiddenAt) {
        this.message = message;
        this.member = member;
        this.hiddenAt = hiddenAt;
    }
}
