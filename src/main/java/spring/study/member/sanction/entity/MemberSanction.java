package spring.study.member.sanction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSanction extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private Member issuedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportAction type;

    @Column(length = 2000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime canceledAt;

    @Builder
    public MemberSanction(Member member, Report report, Member issuedBy, ReportAction type,
                          String reason, LocalDateTime startedAt, LocalDateTime expiredAt) {
        this.member = member;
        this.report = report;
        this.issuedBy = issuedBy;
        this.type = type;
        this.reason = reason;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
    }
}
