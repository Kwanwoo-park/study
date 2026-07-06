package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.facade.CommentFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comment")
public class CommentApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final CommentFacade commentFacade;

    @PostMapping("")
    public ResponseEntity<?> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.saveComment(commentRequestDto, member, request);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCommentList(@RequestParam Long id,
                                            @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                            @RequestParam(defaultValue = "10", name = "limit") int limit,
                                            HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.getList(id, member, cursor, limit);
    }

    @PatchMapping("/update")
    public ResponseEntity<?> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.updateComment(commentRequestDto, member, request);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> commentDelete(@RequestParam Long id,
                                                 @RequestBody(required = false) CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.deleteComment(id, commentRequestDto, member, request);
    }
}
