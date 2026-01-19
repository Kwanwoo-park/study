package spring.study.forbidden.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.forbidden.dto.ForbiddenChangeRequestDto;
import spring.study.forbidden.dto.ForbiddenRequestDto;
import spring.study.forbidden.dto.ForbiddenResponseDto;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/forbidden/word")
public class ForbiddenApiController {
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> forbiddenWordSearch(@RequestParam String word, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();
        List<ForbiddenResponseDto> list;

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            list = forbiddenService.findByWord(word);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        map.put("list", list);
        map.put("result", (long) list.size());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/proposal")
    public ResponseEntity<Map<String, Object>> forbiddenProposalWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();
        List<ForbiddenResponseDto> list;

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            list = forbiddenService.findByStatus(Status.PROPOSAL);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        map.put("list", list);
        map.put("result", (long) list.size());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/examine")
    public ResponseEntity<Map<String, Object>> forbiddenExamineWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();
        List<ForbiddenResponseDto> list;

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            list = forbiddenService.findByStatus(Status.EXAMINE);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        map.put("list", list);
        map.put("result", (long) list.size());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/approval")
    public ResponseEntity<Map<String, Object>> forbiddenApprovalWordSearch(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();
        List<ForbiddenResponseDto> list;

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            list = forbiddenService.findByStatus(Status.APPROVAL);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        map.put("list", list);
        map.put("result", (long) list.size());

        return ResponseEntity.ok(map);
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Long>> forbiddenWordApply(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (requestDto.getWord().isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        try {
            if (forbiddenService.existWord(requestDto.getWord())) {
                map.put("result", -1L);
                return ResponseEntity.ok(map);
            }

            requestDto.setStatus(Status.PROPOSAL);
            map.put("result", forbiddenService.save(requestDto).getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PostMapping("/admin/save")
    public ResponseEntity<Map<String, Long>> forbiddenWordSave(@RequestBody ForbiddenRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (requestDto.getWord().isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        try {
            if (forbiddenService.existWord(requestDto.getWord())) {
                map.put("result", -1L);
                return ResponseEntity.ok(map);
            }

            requestDto.setStatus(Status.APPROVAL);
            map.put("result", forbiddenService.save(requestDto).getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/admin/change/examine")
    public ResponseEntity<Map<String, Integer>> changeToExamine(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            map.put("result", forbiddenService.updateStatus(Status.EXAMINE, requestDto.getIdList()));

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/admin/change/approval")
    public ResponseEntity<Map<String, Integer>> changeToApproval(@RequestBody ForbiddenChangeRequestDto requestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            map.put("result", forbiddenService.updateStatus(Status.APPROVAL, requestDto.getIdList()));

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
