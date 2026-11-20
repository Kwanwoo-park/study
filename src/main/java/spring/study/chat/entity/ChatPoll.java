package spring.study.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_poll", indexes = @Index(name = "idx_chat_poll_room_id", columnList = "room_id,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatPoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @ElementCollection
    @CollectionTable(name = "chat_poll_option", joinColumns = @JoinColumn(name = "poll_id"))
    @OrderColumn(name = "option_index")
    @Column(name = "label", nullable = false, length = 80)
    @BatchSize(size = 20)
    private List<String> options = new ArrayList<>();

    @OneToMany(mappedBy = "poll", cascade = CascadeType.REMOVE)
    private List<ChatPollVote> votes = new ArrayList<>();

    public ChatPoll(ChatRoom room, String title, LocalDateTime endsAt, List<String> options) {
        this.room = room;
        this.title = title;
        this.endsAt = endsAt;
        this.options.addAll(options);
    }

    public boolean isClosed(LocalDateTime now) {
        return !now.isBefore(endsAt);
    }
}
