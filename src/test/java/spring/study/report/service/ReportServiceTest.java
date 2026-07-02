package spring.study.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.dto.ReportResponseDto;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;
import spring.study.report.repository.ReportRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void cancelShouldMarkPendingReportCanceledWhenReporterOwnsIt() {
        Member reporter = createMember(1L, "reporter@test.com");
        Report report = createReport(10L, reporter, ReportStatus.PENDING);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        ReportResponseDto response = reportService.cancel(10L, reporter);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.CANCELED);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.CANCELED);
        assertThat(report.getAction()).isEqualTo(ReportAction.NONE);
        assertThat(report.getReportMemo()).isEqualTo("신고 취소");
    }

    @Test
    void cancelShouldRejectOtherReporter() {
        Member owner = createMember(1L, "owner@test.com");
        Member other = createMember(2L, "other@test.com");
        Report report = createReport(10L, owner, ReportStatus.PENDING);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.cancel(10L, other))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void cancelShouldRejectReportAlreadyUnderReview() {
        Member reporter = createMember(1L, "reporter@test.com");
        Report report = createReport(10L, reporter, ReportStatus.REVIEWING);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.cancel(10L, reporter))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createShouldIgnoreCanceledReportsWhenCheckingDuplicate() {
        Member reporter = createMember(1L, "reporter@test.com");
        ReportRequestDto requestDto = new ReportRequestDto();
        requestDto.setTargetType(ReportTargetType.BOARD);
        requestDto.setTargetId("100");
        requestDto.setReason(ReportReason.SPAM);
        requestDto.setDescription("spam");

        when(reportRepository.existsByReporterAndTargetTypeAndTargetIdAndStatusNot(
                reporter,
                ReportTargetType.BOARD,
                "100",
                ReportStatus.CANCELED
        )).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(20L);
            return report;
        });

        ReportResponseDto response = reportService.create(requestDto, reporter);

        assertThat(response.getId()).isEqualTo(20L);
        verify(reportRepository).existsByReporterAndTargetTypeAndTargetIdAndStatusNot(
                reporter,
                ReportTargetType.BOARD,
                "100",
                ReportStatus.CANCELED
        );
    }

    private Member createMember(Long id, String email) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("pwd")
                .name("member")
                .role(Role.USER)
                .phone("010-0000-000" + id)
                .birth("2000-01-01")
                .profile("profile")
                .build();
    }

    private Report createReport(Long id, Member reporter, ReportStatus status) {
        return Report.builder()
                .id(id)
                .reporter(reporter)
                .status(status)
                .targetType(ReportTargetType.BOARD)
                .reason(ReportReason.SPAM)
                .description("description")
                .targetId("100")
                .action(ReportAction.NONE)
                .build();
    }
}
