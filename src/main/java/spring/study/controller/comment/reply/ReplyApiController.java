package spring.study.controller.comment.reply;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.reply.ReplyRequestDto;
import spring.study.dto.comment.reply.ReplyResponseDto;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Status;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.comment.CommentService;
import spring.study.service.comment.reply.ReplyService;
import spring.study.service.forbidden.ForbiddenService;
import spring.study.service.notification.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final CommentService commentService;
    private final ReplyService replyService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Long> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (!replyRequestDto.getReply().isBlank() || !replyRequestDto.getReply().isEmpty()) {
            if (forbiddenService.findWordList(Status.APPROVAL, replyRequestDto.getReply()))
                return ResponseEntity.ok(-1L);

            Comment comment = commentService.findById(replyRequestDto.getId());

            Member otherMember = comment.getMember();

            Reply result = replyService.replaceReply(replyRequestDto, comment.getMember());

            member.addReply(result);
            comment.addReply(result);

            if (!member.getId().equals(otherMember.getId())) {
                Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다");
                notification.addMember(otherMember);
            }

            session.setAttribute("member", member);

            return ResponseEntity.ok(replyService.save(result).getId());
        }
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReplyResponseDto>> getReply(@RequestParam() Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        List<ReplyResponseDto> reply = commentService.findById(id).getReply().stream().map(ReplyResponseDto::new).toList();

        return ResponseEntity.ok(reply);
    }
}
