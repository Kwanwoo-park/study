package spring.study.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.entity.AccountStatus;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.board.repository.BoardRepository;
import spring.study.comment.repository.CommentRepository;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.dto.ReportResponseDto;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;
import spring.study.report.repository.ReportRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private MemberSanctionRepository memberSanctionRepository;
    @Mock
    private NotificationService notificationService;

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

    @Test
    void createShouldSaveReasonDetailWhenReasonIsEtc() {
        Member reporter = createMember(1L, "reporter@test.com");
        ReportRequestDto requestDto = new ReportRequestDto();
        requestDto.setTargetType(ReportTargetType.BOARD);
        requestDto.setTargetId("100");
        requestDto.setReason(ReportReason.ETC);
        requestDto.setReasonDetail("  other reason  ");
        requestDto.setDescription("description");

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

        assertThat(response.getReason()).isEqualTo(ReportReason.ETC);
        assertThat(response.getReasonDetail()).isEqualTo("other reason");
    }

    @Test
    void createShouldRejectEtcWithoutReasonDetail() {
        Member reporter = createMember(1L, "reporter@test.com");
        ReportRequestDto requestDto = new ReportRequestDto();
        requestDto.setTargetType(ReportTargetType.BOARD);
        requestDto.setTargetId("100");
        requestDto.setReason(ReportReason.ETC);
        requestDto.setReasonDetail(" ");
        requestDto.setDescription("description");

        assertThatThrownBy(() -> reportService.create(requestDto, reporter))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void processShouldApplyWarningAndSaveSanction() {
        Member reporter = createMember(1L, "reporter@test.com");
        Member target = createMember(2L, "target@test.com");
        Member admin = createMember(3L, "admin@test.com");
        admin.setRole(Role.ADMIN);
        Report report = createReport(10L, reporter, ReportStatus.REVIEWING);
        report.setTargetType(ReportTargetType.MEMBER);
        report.setTargetId(target.getEmail());
        ReportProcessRequestDto request = processRequest(ReportAction.WARNING, null);

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(memberRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(memberSanctionRepository.existsByReportId(10L)).thenReturn(false);

        reportService.process(10L, request, admin);

        assertThat(target.getWarningCount()).isEqualTo(1);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(memberSanctionRepository).save(any(MemberSanction.class));
    }

    @Test
    void processShouldSuspendMemberUntilRequestedTime() {
        Member reporter = createMember(1L, "reporter@test.com");
        Member target = createMember(2L, "target@test.com");
        Member admin = createMember(3L, "admin@test.com");
        LocalDateTime until = LocalDateTime.now().plusDays(7);
        Report report = createReport(10L, reporter, ReportStatus.REVIEWING);
        report.setTargetType(ReportTargetType.MEMBER);
        report.setTargetId(target.getEmail());

        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));
        when(memberRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));

        reportService.process(10L, processRequest(ReportAction.TEMPORARY_SUSPEND, until), admin);

        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(target.getSuspendedUntil()).isEqualTo(until);
        verify(notificationService).createNotification(
                eq(target),
                argThat(message -> message.contains("금지일자:")
                        && message.contains(until.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        && message.contains("사유: moderation reason")),
                eq(Group.ADMIN)
        );
    }

    @Test
    void processShouldRejectExpiredSuspensionDate() {
        Member reporter = createMember(1L, "reporter@test.com");
        Report report = createReport(10L, reporter, ReportStatus.REVIEWING);
        when(reportRepository.findById(10L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.process(
                10L,
                processRequest(ReportAction.TEMPORARY_SUSPEND, LocalDateTime.now().minusMinutes(1)),
                createMember(3L, "admin@test.com")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void findHistoryShouldOnlyRequestCompletedStatuses() {
        when(reportRepository.findByStatusIn(
                org.mockito.ArgumentMatchers.eq(List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED)),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        reportService.findHistory(null, 0, 10);

        verify(reportRepository).findByStatusIn(
                org.mockito.ArgumentMatchers.eq(List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED)),
                any(Pageable.class)
        );
    }

    @Test
    void findHistoryShouldRejectPendingStatus() {
        assertThatThrownBy(() -> reportService.findHistory(ReportStatus.PENDING, 0, 10))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private ReportProcessRequestDto processRequest(ReportAction action, LocalDateTime suspendedUntil) {
        ReportProcessRequestDto request = new ReportProcessRequestDto();
        request.setStatus(ReportStatus.RESOLVED);
        request.setAction(action);
        request.setReportMemo("moderation reason");
        request.setSuspendedUntil(suspendedUntil);
        return request;
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
