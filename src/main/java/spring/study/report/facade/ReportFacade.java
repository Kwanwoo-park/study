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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportFacade {
    private final ReportService reportService;

    public ResponseEntity<?> create(ReportRequestDto requestDto, Member reporter) {
        ReportResponseDto report = reportService.create(requestDto, reporter);

        return reportResponse(report);
    }

    public ResponseEntity<?> findByReporter(Member reporter, int page, int size) {
        return pageResponse(reportService.findByReporter(reporter, page, size));
    }

    public ResponseEntity<?> findAll(ReportStatus status, int page, int size) {
        return pageResponse(reportService.findAll(status, page, size));
    }

    public ResponseEntity<?> findById(Long id) {
        return reportResponse(reportService.findById(id));
    }

    public ResponseEntity<?> process(Long id, ReportProcessRequestDto requestDto) {
        return reportResponse(reportService.process(id, requestDto));
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
}
