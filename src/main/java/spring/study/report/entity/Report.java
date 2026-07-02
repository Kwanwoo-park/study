package spring.study.report.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "report")
public class Report extends BasetimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @JoinColumn(name = "reporter_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member reporter;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportTargetType targetType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @NotNull
    @Column(length = 1000)
    private String description;

    @NotNull
    @Column(length = 100)
    private String targetId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportAction action;

    @Column(length = 2000)
    private String reportMemo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String snapshot;

    @Builder
    public Report(
            Long id,
            Member reporter,
            ReportStatus status,
            ReportTargetType targetType,
            ReportReason reason,
            String description,
            String targetId,
            ReportAction action,
            String reportMemo,
            String snapshot
    ) {
        this.id = id;
        this.reporter = reporter;
        this.status = status == null ? ReportStatus.PENDING : status;
        this.targetType = targetType;
        this.reason = reason;
        this.description = description;
        this.targetId = targetId;
        this.action = action == null ? ReportAction.NONE : action;
        this.reportMemo = reportMemo;
        this.snapshot = snapshot;
    }

    public void updateProcess(ReportStatus status, ReportAction action, String reportMemo) {
        this.status = status;
        this.action = action;
        this.reportMemo = reportMemo;
        changeUpdateTime(LocalDateTime.now());
    }

    public void cancel() {
        this.status = ReportStatus.CANCELED;
        this.action = ReportAction.NONE;
        this.reportMemo = "신고 취소";
        changeUpdateTime(LocalDateTime.now());
    }
}
