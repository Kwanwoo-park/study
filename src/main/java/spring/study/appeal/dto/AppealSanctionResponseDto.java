package spring.study.appeal.dto;

import lombok.Getter;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;

import java.time.LocalDateTime;

@Getter
public class AppealSanctionResponseDto {
    private final Long sanctionId;
    private final Long reportId;
    private final ReportTargetType targetType;
    private final ReportReason reportReason;
    private final ReportStatus reportStatus;
    private final String reportDescription;
    private final ReportAction sanctionType;
    private final String sanctionReason;
    private final LocalDateTime startedAt;
    private final LocalDateTime expiredAt;

    public AppealSanctionResponseDto(MemberSanction sanction) {
        this.sanctionId = sanction.getId();
        this.reportId = sanction.getReport().getId();
        this.targetType = sanction.getReport().getTargetType();
        this.reportReason = sanction.getReport().getReason();
        this.reportStatus = sanction.getReport().getStatus();
        this.reportDescription = sanction.getReport().getDescription();
        this.sanctionType = sanction.getType();
        this.sanctionReason = sanction.getReason();
        this.startedAt = sanction.getStartedAt();
        this.expiredAt = sanction.getExpiredAt();
    }
}
