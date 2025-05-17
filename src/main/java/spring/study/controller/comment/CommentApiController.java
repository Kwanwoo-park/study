package spring.study.controller.comment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.entity.board.Board;
import spring.study.entity.comment.Comment;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Status;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.board.BoardService;
import spring.study.service.comment.CommentService;
import spring.study.service.forbidden.ForbiddenService;
import spring.study.service.notification.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Long> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            List<Forbidden> wordList = forbiddenService.findWordList(Status.APPROVAL);

            for (Forbidden word : wordList) {
                if (commentRequestDto.getComments().contains(word.getWord()))
                    return ResponseEntity.ok(-1L);
            }

            Board board = boardService.findById(commentRequestDto.getId());

            Member otherMember = board.getMember();

            Comment comment = Comment.builder()
                    .comments(commentRequestDto.getComments())
                    .build();

            member.addComment(comment);
            board.addComment(comment);

            if (!member.getId().equals(otherMember.getId())) {
                Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 게시물에 댓글을 작성하였습니다");
                notification.addMember(otherMember);
            }

            session.setAttribute("member", member);

            return ResponseEntity.ok(commentService.save(comment).getId());
        }
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @PatchMapping("/update")
    public ResponseEntity<Integer> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            List<Forbidden> wordList = forbiddenService.findWordList(Status.APPROVAL);

            for (Forbidden word : wordList) {
                if (commentRequestDto.getComments().contains(word.getWord()))
                    return ResponseEntity.ok(-1);
            }

            return ResponseEntity.ok(commentService.updateComments(commentRequestDto.getId(), commentRequestDto.getComments()));
        }
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Comment> commentDelete(@RequestParam Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
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
