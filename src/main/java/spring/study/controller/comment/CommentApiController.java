package spring.study.controller.comment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.board.Board;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.board.BoardService;
import spring.study.service.comment.CommentService;
import spring.study.service.notification.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Comment> commentAction(@RequestParam() Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Member otherMember = board.getMember();

        Comment comment = Comment.builder()
                .comments(commentRequestDto.getComments())
                .build();

        member.addComment(comment);
        board.addComment(comment);

        Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 게시물에 댓글을 작성하였습니다");
        notification.addMember(otherMember);

        session.setAttribute("member", member);

        return ResponseEntity.ok(commentService.save(comment));
    }

    @PatchMapping("/update")
    public ResponseEntity<Integer> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        return ResponseEntity.ok(commentService.updateComments(commentRequestDto.getId(), commentRequestDto.getComments()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Comment> commentDelete(@RequestParam Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);
        Comment comment = commentService.findById(commentRequestDto.getId());

        member.removeComment(comment);
        board.removeComment(comment);

        commentService.deleteById(commentRequestDto.getId());

        session.setAttribute("member", member);

        return ResponseEntity.ok(comment);
    }
}
