package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.facade.CommentFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comment")
public class CommentApiController {
    private final SessionManager sessionManager;
    private final CommentFacade commentFacade;

    @PostMapping("")
    public ResponseEntity<?> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return commentFacade.getCommentList(commentRequestDto, member, request);
    }

    @PatchMapping("/update")
    public ResponseEntity<?> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return commentFacade.updateComment(commentRequestDto, member, request);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> commentDelete(@RequestParam Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return commentFacade.deleteComment(id, commentRequestDto, member, request);
    }
}
