package spring.study.report.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.dto.ReportResponseDto;
import spring.study.report.entity.ReportStatus;
import spring.study.report.service.ReportService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.member.service.MemberService;
import spring.study.member.sanction.service.MemberSanctionService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.chat.service.ChatMessageService;
import spring.study.notification.service.NotificationService;
import spring.study.notification.entity.Group;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportFacade {
    private static final DateTimeFormatter SANCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ReportService reportService;
    private final MemberService memberService;
    private final BoardService boardService;
    private final CommentService commentService;
    private final ChatMessageService chatMessageService;
    private final MemberSanctionService memberSanctionService;
    private final NotificationService notificationService;

    @Transactional
    public ResponseEntity<?> create(ReportRequestDto requestDto, Member reporter) {
        Report report = reportService.create(requestDto, reporter);
        notifyAdministrators(report);

        return reportResponse(new ReportResponseDto(report));
    }

    public ResponseEntity<?> findByReporter(Member reporter, int page, int size) {
        return pageResponse(reportService.findByReporter(reporter, page, size));
    }

    public ResponseEntity<?> findAll(ReportStatus status, int page, int size) {
        return pageResponse(reportService.findAll(status, page, size));
    }

    public ResponseEntity<?> findAllByStatus(ReportStatus status) {
        return listResponse(reportService.findAllByStatus(status));
    }

    public ResponseEntity<?> findHistory(ReportStatus status, int page, int size) {
        return pageResponse(reportService.findHistory(status, page, size));
    }

    public ResponseEntity<?> findById(Long id) {
        return reportResponse(reportService.findById(id));
    }

    @Transactional
    public ResponseEntity<?> process(Long id, ReportProcessRequestDto requestDto, Member admin) {
        Report report = reportService.process(id, requestDto);
        if (requestDto.getStatus() == ReportStatus.RESOLVED && isMemberSanction(requestDto.getAction())) {
            Member target = resolveTargetMember(report);
            memberSanctionService.apply(report, requestDto, admin, target);
            if (requestDto.getAction() != ReportAction.WARNING) {
                notificationService.createNotification(target, createSanctionNotificationMessage(requestDto), Group.ADMIN);
            }
        }
        return reportResponse(new ReportResponseDto(report));
    }

    public ResponseEntity<?> cancel(Long id, Member reporter) {
        return reportResponse(reportService.cancel(id, reporter));
    }

    private ResponseEntity<?> reportResponse(ReportResponseDto report) {
        return ResponseEntity.ok(Map.of(
                "result", report.getId(),
                "report", report
        ));
    }

    private ResponseEntity<?> pageResponse(Page<ReportResponseDto> reports) {
        return ResponseEntity.ok(Map.of(
                "result", reports.getTotalElements(),
                "list", reports.getContent(),
                "page", reports.getNumber(),
                "size", reports.getSize(),
                "totalPages", reports.getTotalPages(),
                "totalElements", reports.getTotalElements()
        ));
    }

    private ResponseEntity<?> listResponse(List<ReportResponseDto> reports) {
        return ResponseEntity.ok(Map.of(
                "result", reports.size(),
                "list", reports
        ));
    }
    private void notifyAdministrators(Report report) {
        String targetName = switch (report.getTargetType()) {
            case MEMBER -> "회원";
            case BOARD -> "게시글";
            case COMMENT -> "댓글";
            case CHAT_MESSAGE -> "채팅 메시지";
        };
        String reporterName = report.getReporter().getName() == null
                || report.getReporter().getName().isBlank()
                ? report.getReporter().getEmail()
                : report.getReporter().getName();
        String message = "새 " + targetName + " 신고가 접수되었습니다. 신고자: " + reporterName;
        String url = "/admin/report?reportId=" + report.getId();

        memberService.findAdministrators().forEach(admin ->
                notificationService.createNotification(admin, message, Group.ADMIN, url)
        );
    }

    private boolean isMemberSanction(ReportAction action) {
        return action == ReportAction.WARNING
                || action == ReportAction.TEMPORARY_SUSPEND
                || action == ReportAction.PERMANENT_BAN;
    }

    private String createSanctionNotificationMessage(ReportProcessRequestDto requestDto) {
        String startedAt = LocalDateTime.now().format(SANCTION_DATE_FORMAT);
        String sanctionPeriod = requestDto.getAction() == ReportAction.TEMPORARY_SUSPEND
                ? startedAt + " ~ " + requestDto.getSuspendedUntil().format(SANCTION_DATE_FORMAT)
                : startedAt + " (영구)";
        String reason = requestDto.getReportMemo() == null || requestDto.getReportMemo().isBlank()
                ? "신고 처리에 따른 제재"
                : requestDto.getReportMemo().trim();

        return "금지일자: " + sanctionPeriod + " / 사유: " + reason;
    }

    private Member resolveTargetMember(Report report) {
        try {
            return switch (report.getTargetType()) {
                case MEMBER -> memberService.findMember(report.getTargetId());
                case BOARD -> boardService.findById(Long.valueOf(report.getTargetId())).getMember();
                case COMMENT -> commentService.findById(Long.valueOf(report.getTargetId())).getMember();
                case CHAT_MESSAGE -> chatMessageService.findById(report.getTargetId()).getMember();
            };
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신고 대상 식별자가 올바르지 않습니다");
        }
    }
}
