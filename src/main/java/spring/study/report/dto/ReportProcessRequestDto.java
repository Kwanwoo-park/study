package spring.study.report.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReportProcessRequestDto {
    @NotNull
    private ReportStatus status;

    @NotNull
    private ReportAction action;

    @Size(max = 2000)
    private String reportMemo;

    private LocalDateTime suspendedUntil;
}
