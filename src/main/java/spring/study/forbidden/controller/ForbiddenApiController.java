package spring.study.forbidden.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.forbidden.dto.ForbiddenChangeRequestDto;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.dto.ForbiddenResponseDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/forbidden/word")
public class ForbiddenApiController {
    private final ForbiddenService forbiddenService;

    @GetMapping("/search")
    public ResponseEntity<List<ForbiddenResponseDto>> forbiddenWordSearch(@RequestParam String word, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        return ResponseEntity.ok(forbiddenService.findByWord(word));
    }

    @GetMapping("/proposal")
    public ResponseEntity<List<ForbiddenResponseDto>> forbiddenProposalWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        return ResponseEntity.ok(forbiddenService.findByStatus(Status.PROPOSAL));
    }

    @GetMapping("/examine")
    public ResponseEntity<List<ForbiddenResponseDto>> forbiddenExamineWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        return ResponseEntity.ok(forbiddenService.findByStatus(Status.EXAMINE));
    }

    @GetMapping("/approval")
    public ResponseEntity<List<ForbiddenResponseDto>> forbiddenApprovalWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        return ResponseEntity.ok(forbiddenService.findByStatus(Status.APPROVAL));
    }

    @PostMapping("/apply")
    public ResponseEntity<Long> forbiddenWordApply(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (requestDto.getWord().isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        if (!forbiddenService.existWord(requestDto.getWord()))
            return ResponseEntity.ok(-1L);

        requestDto.setStatus(Status.PROPOSAL);

        return ResponseEntity.ok(forbiddenService.save(requestDto).getId());
    }

    @PostMapping("/admin/save")
    public ResponseEntity<Long> forbiddenWordSave(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (requestDto.getWord().isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        if (!forbiddenService.existWord(requestDto.getWord()))
            return ResponseEntity.ok(-1L);

        requestDto.setStatus(Status.APPROVAL);

        return ResponseEntity.ok(forbiddenService.save(requestDto).getId());
    }

    @PatchMapping("/admin/change/examine")
    public ResponseEntity<Integer> changeToExamine(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        int result = forbiddenService.updateStatus(Status.EXAMINE, requestDto.getIdList());

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/admin/change/approval")
    public ResponseEntity<Integer> changeToApporval(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        int result = forbiddenService.updateStatus(Status.APPROVAL, requestDto.getIdList());

        return ResponseEntity.ok(result);
    }
}
