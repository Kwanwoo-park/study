package spring.study.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ReportResponseDto {
    private Long id;
    private Long reporterId;
    private String reporterEmail;
    private String reporterName;
    private ReportStatus status;
    private ReportTargetType targetType;
    private ReportReason reason;
    private String reasonDetail;
    private String description;
    private String targetId;
    private ReportAction action;
    private String reportMemo;
    private String snapshot;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;

    public ReportResponseDto(Report report) {
        this.id = report.getId();
        this.reporterId = report.getReporter().getId();
        this.reporterEmail = report.getReporter().getEmail();
        this.reporterName = report.getReporter().getName();
        this.status = report.getStatus();
        this.targetType = report.getTargetType();
        this.reason = report.getReason();
        this.reasonDetail = report.getReasonDetail();
        this.description = report.getDescription();
        this.targetId = report.getTargetId();
        this.action = report.getAction();
        this.reportMemo = report.getReportMemo();
        this.snapshot = report.getSnapshot();
        this.registerTime = report.getRegisterTime();
        this.updateTime = report.getUpdateTime();
    }
}
