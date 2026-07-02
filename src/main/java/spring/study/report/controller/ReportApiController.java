package spring.study.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.report.dto.ReportRequestDto;
import spring.study.report.facade.ReportFacade;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportApiController {
    private final ReportFacade reportFacade;
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ReportRequestDto requestDto, HttpServletRequest request) {
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

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return reportFacade.cancel(id, member);
    }
}
