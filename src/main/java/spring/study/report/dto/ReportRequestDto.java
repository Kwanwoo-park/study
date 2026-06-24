package spring.study.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.member.entity.Member;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {
    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private ReportReason reason;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String targetId;

    private String snapshot;

    public Report toEntity(Member reporter) {
        return Report.builder()
                .reporter(reporter)
                .status(ReportStatus.PENDING)
                .targetType(targetType)
                .reason(reason)
                .description(description)
                .targetId(targetId)
                .action(ReportAction.NONE)
                .snapshot(snapshot)
                .build();
    }
}
