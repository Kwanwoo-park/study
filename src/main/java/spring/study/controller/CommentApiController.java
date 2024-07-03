package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;

    @PostMapping("/{bid}/action")
    public Comment commentAction(@PathVariable Long bid,
                              @RequestBody CommentRequestDto commentRequestDto,
                              HttpSession session) {

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(bid);

        Comment comment = Comment.builder()
                .comments(commentRequestDto.getComments())
                .build();

        member.addComment(comment);
        board.addComment(comment);

        session.setAttribute("member", member);

        return commentService.save(comment);
    }
}
