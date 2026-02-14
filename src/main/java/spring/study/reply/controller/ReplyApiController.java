package spring.study.reply.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.common.service.SessionService;
import spring.study.member.entity.Member;
import spring.study.reply.facade.ReplyFacade;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final SessionService sessionService;
    private final ReplyFacade replyFacade;

    @PostMapping("")
    public ResponseEntity<?> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return replyFacade.saveReply(replyRequestDto, member, request);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getReply(@RequestParam() Long id, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return replyFacade.getList(id);
    }
}
