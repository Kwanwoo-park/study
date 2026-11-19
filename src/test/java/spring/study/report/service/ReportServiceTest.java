package spring.study.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.entity.MemberStatus;
import spring.study.member.repository.MemberRepository;
import spring.study.sanction.entity.MemberSanction;
import spring.study.sanction.repository.MemberSanctionRepository;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;
import spring.study.report.repository.ReportRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.sanction.service.MemberSanctionService;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    private MemberSanctionRepository memberSanctionRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ReportService reportService;
    private spring.study.report.facade.ReportFacade reportFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUpLayers() {
        reportService = new ReportService(reportRepository);
        reportFacade = new spring.study.report.facade.ReportFacade(reportService,
                new spring.study.member.service.MemberService(memberRepository, eventPublisher),
                org.mockito.Mockito.mock(spring.study.board.service.BoardService.class),
                org.mockito.Mockito.mock(spring.study.comment.service.CommentService.class),
                org.mockito.Mockito.mock(spring.study.chat.service.ChatMessageService.class),
                new MemberSanctionService(memberSanctionRepository, eventPublisher), notificationService);
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

        when(reportRepository.findForUpdateByIdAndReporterIsNotNull(10L)).thenReturn(Optional.of(report));
        when(memberRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
        when(memberSanctionRepository.existsByReportId(10L)).thenReturn(false);

        reportFacade.process(10L, request, admin);

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

        when(reportRepository.findForUpdateByIdAndReporterIsNotNull(10L)).thenReturn(Optional.of(report));
        when(memberRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));

        reportFacade.process(10L, processRequest(ReportAction.TEMPORARY_SUSPEND, until), admin);

        assertThat(target.getAccountStatus()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(target.getSuspendedUntil()).isEqualTo(until);
        verify(notificationService).createNotification(
                eq(target),
                argThat(message -> message.contains("금지일자:")
                        && message.contains(until.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        && message.contains("사유: moderation reason")),
                eq(Group.ADMIN)
        );
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
