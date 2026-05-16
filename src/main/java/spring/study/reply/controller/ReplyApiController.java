package spring.study.reply.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.reply.facade.ReplyFacade;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final ReplyFacade replyFacade;

    @PostMapping("")
    public ResponseEntity<?> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return replyFacade.saveReply(replyRequestDto, member, request);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getReply(@RequestParam() Long id,
                                      @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                      @RequestParam(defaultValue = "10", name = "limit") int limit,
                                      HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return replyFacade.getList(id, member, cursor, limit);
    }
}
