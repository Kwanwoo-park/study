package spring.study.reply.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.reply.dto.ReplyRequestDto;
import spring.study.common.facade.CommonFacade;
import spring.study.common.facade.ModerationFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.reply.facade.ReplyFacade;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final ReplyFacade replyFacade;
    private final ModerationFacade moderationFacade;

    @PostMapping("")
    public ResponseEntity<?> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> validation = validateContent(replyRequestDto.getReply(), member, response, -99L);
        if (validation != null) return validation;

        return replyFacade.saveReply(replyRequestDto, member);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getReply(@RequestParam() Long id, @RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "10", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return replyFacade.getList(id, member, cursor, limit);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateReply(@PathVariable Long id, @RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> permission = replyFacade.validateUpdate(id, member);
        if (permission != null) return permission;

        ResponseEntity<?> validation = validateContent(replyRequestDto.getReply(), member, response, -10L);
        if (validation != null) return validation;

        return replyFacade.updateReply(id, replyRequestDto, member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReply(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        return replyFacade.deleteReply(id, member);
    }

    private ResponseEntity<?> validateContent(String content, Member member, HttpServletResponse response, long emptyResult) {
        int risk = moderationFacade.validate(content, member, response);
        if (risk == 0) return null;

        return ResponseEntity.status(risk == -99 ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN).body(Map.of(
                "result", risk == -99 ? emptyResult : -risk,
                "message", risk == -99 ? "답글이 없습니다" : "금칙어를 사용하였습니다"
        ));
    }
}
