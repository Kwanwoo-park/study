package spring.study.report.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.event.MemberChangedEvent;
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.board.repository.BoardRepository;
import spring.study.comment.repository.CommentRepository;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.dto.ReportResponseDto;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportAction;
import spring.study.report.repository.ReportRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;

    @Transactional
    public Report create(ReportRequestDto requestDto, Member reporter) {
        validateReasonDetail(requestDto);

        boolean alreadyReported = reportRepository.existsByReporterAndTargetTypeAndTargetIdAndStatusNot(
                reporter,
                requestDto.getTargetType(),
                requestDto.getTargetId(),
                ReportStatus.CANCELED
        );

        if (alreadyReported) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다");
        }

        Report report = reportRepository.save(requestDto.toEntity(reporter));
        return report;
    }



    private void validateReasonDetail(ReportRequestDto requestDto) {
        if (requestDto.getReason() == ReportReason.ETC && requestDto.getNormalizedReasonDetail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기타 신고 사유를 입력해주세요");
        }
    }

    @Transactional(readOnly = true)
    public ReportResponseDto findById(Long id) {
        return new ReportResponseDto(findEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponseDto> findByReporter(Member reporter, int page, int size) {
        return reportRepository.findByReporter(reporter, createPageRequest(page, size))
                .map(ReportResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponseDto> findAll(ReportStatus status, int page, int size) {
        PageRequest pageable = createPageRequest(page, size);
        Page<Report> reports = status == null
                ? reportRepository.findAll(pageable)
                : reportRepository.findByStatus(status, pageable);

        return reports.map(ReportResponseDto::new);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponseDto> findHistory(ReportStatus status, int page, int size) {
        PageRequest pageable = createPageRequest(page, size);
        Page<Report> reports;

        if (status == null) {
            reports = reportRepository.findByStatusIn(
                    List.of(ReportStatus.RESOLVED, ReportStatus.REJECTED),
                    pageable
            );
        } else if (status == ReportStatus.RESOLVED || status == ReportStatus.REJECTED) {
            reports = reportRepository.findByStatus(status, pageable);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리 완료 또는 반려 상태만 조회할 수 있습니다");
        }

        return reports.map(ReportResponseDto::new);
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDto> findAllByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status, Sort.by("registerTime").descending())
                .stream()
                .map(ReportResponseDto::new)
                .toList();
    }

    @Transactional
    public Report process(Long id, ReportProcessRequestDto requestDto) {
        Report report = reportRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다"));

        validateProcess(report, requestDto);

        report.updateProcess(
                requestDto.getStatus(),
                requestDto.getAction(),
                requestDto.getReportMemo()
        );

        return report;
    }

    private void validateProcess(Report report, ReportProcessRequestDto requestDto) {
        if (report.getStatus() != ReportStatus.PENDING && report.getStatus() != ReportStatus.REVIEWING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 처리가 끝난 신고입니다");
        }
        if (requestDto.getStatus() == ReportStatus.REJECTED && requestDto.getAction() != ReportAction.NONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반려 신고에는 제재를 적용할 수 없습니다");
        }
        if (requestDto.getAction() == ReportAction.TEMPORARY_SUSPEND
                && (requestDto.getSuspendedUntil() == null
                || !requestDto.getSuspendedUntil().isAfter(LocalDateTime.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "임시 정지 종료 시각은 현재보다 이후여야 합니다");
        }
    }











    @Transactional
    public ReportResponseDto cancel(Long id, Member reporter) {
        Report report = reportRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 신고입니다"));

        if (!report.getReporter().getId().equals(reporter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 신고한 내역만 취소할 수 있습니다");
        }

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "접수 대기 중인 신고만 취소할 수 있습니다");
        }

        report.cancel();

        return new ReportResponseDto(report);
    }

    @Transactional
    public void deleteByReporter(Member reporter) {
        reportRepository.deleteByReporter(reporter);
    }

    private Report findEntity(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다"));
    }

    private PageRequest createPageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(safePage, safeSize, Sort.by("registerTime").descending());
    }
}
