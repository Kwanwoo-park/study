package spring.study.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.report.dto.ReportProcessRequestDto;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.entity.ReportStatus;
import spring.study.report.facade.ReportFacade;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportApiController {
    private final ReportFacade reportFacade;
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ReportRequestDto requestDto,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return reportFacade.create(requestDto, member);
    }

    @GetMapping("/my")
    public ResponseEntity<?> findMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return reportFacade.findByReporter(member, page, size);
    }

    @GetMapping("/admin")
    public ResponseEntity<?> findReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.findAll(status, page, size);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> findReport(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.findById(id);
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> process(
            @PathVariable Long id,
            @Valid @RequestBody ReportProcessRequestDto requestDto,
            HttpServletRequest request
    ) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            request.getSession(false).invalidate();
            return commonFacade.wrongAccess();
        }

        return reportFacade.process(id, requestDto);
    }
}
