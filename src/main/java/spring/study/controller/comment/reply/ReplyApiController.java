package spring.study.controller.comment.reply;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.comment.reply.ReplyRequestDto;
import spring.study.dto.comment.reply.ReplyResponseDto;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.comment.CommentService;
import spring.study.service.comment.reply.ReplyService;
import spring.study.service.notification.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final CommentService commentService;
    private final ReplyService replyService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Reply> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");
        Comment comment = commentService.findById(replyRequestDto.getId());

        Member otherMember = comment.getMember();

        String reply = replyRequestDto.getReply().replace("@"+comment.getMember().getName()+" ", "");

        Reply result = Reply.builder()
                .reply(reply)
                .build();

        member.addReply(result);
        comment.addReply(result);

        if (!member.getId().equals(otherMember.getId())) {
            Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다");
            notification.addMember(otherMember);
        }

        session.setAttribute("member", member);

        return ResponseEntity.ok(replyService.save(result));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReplyResponseDto>> getReply(@RequestParam() Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        List<ReplyResponseDto> reply = commentService.findById(id).getReply().stream().map(ReplyResponseDto::new).toList();

        return ResponseEntity.ok(reply);
    }
}
