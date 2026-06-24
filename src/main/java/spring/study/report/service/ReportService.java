package spring.study.report.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.member.entity.Member;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.dto.ReportResponseDto;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportStatus;
import spring.study.report.repository.ReportRepository;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ReportRepository reportRepository;

    @Transactional
    public ReportResponseDto create(ReportRequestDto requestDto, Member reporter) {
        boolean alreadyReported = reportRepository.existsByReporterAndTargetTypeAndTargetId(
                reporter,
                requestDto.getTargetType(),
                requestDto.getTargetId()
        );

        if (alreadyReported) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 신고한 대상입니다");
        }

        return new ReportResponseDto(reportRepository.save(requestDto.toEntity(reporter)));
    }

    public ReportResponseDto findById(Long id) {
        return new ReportResponseDto(findEntity(id));
    }

    public Page<ReportResponseDto> findByReporter(Member reporter, int page, int size) {
        return reportRepository.findByReporter(reporter, createPageRequest(page, size))
                .map(ReportResponseDto::new);
    }

    public Page<ReportResponseDto> findAll(ReportStatus status, int page, int size) {
        PageRequest pageable = createPageRequest(page, size);
        Page<Report> reports = status == null
                ? reportRepository.findAll(pageable)
                : reportRepository.findByStatus(status, pageable);

        return reports.map(ReportResponseDto::new);
    }

    @Transactional
    public ReportResponseDto process(Long id, ReportProcessRequestDto requestDto) {
        Report report = findEntity(id);

        report.updateProcess(
                requestDto.getStatus(),
                requestDto.getAction(),
                requestDto.getReportMemo()
        );

        return new ReportResponseDto(report);
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
