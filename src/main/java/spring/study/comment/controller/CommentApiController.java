package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.facade.CommentFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.facade.ModerationFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comment")
public class CommentApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final CommentFacade commentFacade;
    private final ModerationFacade moderationFacade;

    @PostMapping("")
    public ResponseEntity<?> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> validation = validateContent(commentRequestDto.getComments(), member, response);
        if (validation != null) return validation;

        return commentFacade.saveComment(commentRequestDto, member);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getCommentList(@RequestParam Long id, @RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "10", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.getList(id, member, cursor, limit);
    }

    @PatchMapping("/update")
    public ResponseEntity<?> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> permission = commentFacade.validateUpdate(commentRequestDto.getId(), member);
        if (permission != null) return permission;

        ResponseEntity<?> validation = validateContent(commentRequestDto.getComments(), member, response);
        if (validation != null) return validation;

        return commentFacade.updateComment(commentRequestDto, member);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> commentDelete(@RequestParam Long id, @RequestBody(required = false) CommentRequestDto commentRequestDto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return commentFacade.deleteComment(id, commentRequestDto, member, request);
    }

    private ResponseEntity<?> validateContent(String content, Member member, HttpServletResponse response) {
        int risk = moderationFacade.validate(content, member, response);
        if (risk == 0) return null;

        if (risk == -99)
            return ResponseEntity.badRequest().body(Map.of("result", -10L, "message", "댓글이 입력되지 않았습니다"));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", -(long) risk, "message", "금칙어를 사용하였습니다"));
    }
}
