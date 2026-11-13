package spring.study.memo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

@Getter
@Entity
@Table(name = "admin_memo", indexes = @Index(name = "idx_admin_memo_owner_pinned_updated", columnList = "owner_id, pinned, update_time, id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_memo_creation_key", columnNames = "creation_key"))
@AttributeOverrides({
        @AttributeOverride(name = "registerTime", column = @Column(name = "register_time")),
        @AttributeOverride(name = "updateTime", column = @Column(name = "update_time"))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    // Makes retries of the first autosave idempotent, including a lost HTTP response.
    @Column(name = "creation_key", length = 36)
    private String creationKey;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoColor color;

    @Column(nullable = false)
    private boolean pinned;

    @Version
    @Column(nullable = false)
    private long version;

    @Builder
    public Memo(Long id, Member owner, String creationKey, String title, String content, MemoColor color, boolean pinned) {
        this.id = id;
        this.owner = owner;
        this.creationKey = creationKey;
        revise(title, content, color, pinned);
    }

    public void revise(String title, String content, MemoColor color, boolean pinned) {
        this.title = title;
        this.content = content;
        changeSettings(color, pinned);
    }

    public void changeSettings(MemoColor color, boolean pinned) {
        this.color = color;
        this.pinned = pinned;
    }
}
