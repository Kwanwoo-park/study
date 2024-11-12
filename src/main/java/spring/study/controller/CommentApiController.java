package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;

    @PostMapping("")
    public ResponseEntity<Comment> commentAction(@RequestParam() Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Comment comment = Comment.builder()
                .comments(commentRequestDto.getComments())
                .build();

        member.addComment(comment);
        board.addComment(comment);

        session.setAttribute("member", member);

        return ResponseEntity.ok(commentService.save(comment));
    }
}
