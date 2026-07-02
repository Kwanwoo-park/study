package spring.study.forbidden.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.facade.ForbiddenFacade;
import spring.study.member.entity.Member;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/forbidden/word")
public class ForbiddenApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final ForbiddenFacade forbiddenFacade;

    @GetMapping("/search")
    public ResponseEntity<?> forbiddenWordSearch(@RequestParam String word, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return forbiddenFacade.search(word);
    }

    @GetMapping("/proposal")
    public ResponseEntity<?> forbiddenProposalWordSearch(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return forbiddenFacade.getStatus(Status.PROPOSAL);
    }

    @GetMapping("/examine")
    public ResponseEntity<?> forbiddenExamineWordSearch(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return forbiddenFacade.getStatus(Status.EXAMINE);
    }

    @GetMapping("/approval")
    public ResponseEntity<?> forbiddenApprovalWordSearch(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return forbiddenFacade.getStatus(Status.APPROVAL);
    }

    @PostMapping("/apply")
    public ResponseEntity<?> forbiddenWordApply(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return forbiddenFacade.wordApply(requestDto);
    }

}
