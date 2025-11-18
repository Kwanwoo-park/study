package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Notification;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.notification.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Long> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            int risk = forbiddenService.findWordList(Status.APPROVAL, commentRequestDto.getComments());

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3L);
                }

                return ResponseEntity.ok(-1L);
            }

            Board board = boardService.findById(commentRequestDto.getId());

            Member otherMember = board.getMember();

            Comment comment = commentService.save(commentRequestDto, member, board);

            if (!member.getId().equals(otherMember.getId())) {
                notificationService.createNotification(otherMember, member.getName() + "님이 게시물에 댓글을 작성하였습니다").addMember(otherMember);
            }

            session.setAttribute("member", member);

            return ResponseEntity.ok(comment.getId());
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

        Member member = (Member) session.getAttribute("member");

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            int risk = forbiddenService.findWordList(Status.APPROVAL, commentRequestDto.getComments());

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3);
                }

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
        Comment comment = commentService.findById(commentRequestDto.getId(), member, board);

        commentService.deleteById(commentRequestDto.getId());

        session.setAttribute("member", member);

        return ResponseEntity.ok(comment);
    }
}
