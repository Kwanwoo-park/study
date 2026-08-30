package spring.study.appeal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;
import spring.study.member.sanction.entity.MemberSanction;

@Getter
@Entity
@Table(name = "member_appeal")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appeal extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sanction_id")
    private MemberSanction relatedSanction;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppealStatus status;

    @Builder
    public Appeal(Long id, Member member, MemberSanction relatedSanction, String title,
                  String content, AppealStatus status) {
        this.id = id;
        this.member = member;
        this.relatedSanction = relatedSanction;
        this.title = title;
        this.content = content;
        this.status = status == null ? AppealStatus.PENDING : status;
    }

    public void accept() {
        status = AppealStatus.ACCEPTED;
    }
}
