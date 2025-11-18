package spring.study.reply.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.comment.dto.reply.ReplyResponseDto;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.reply.entity.Reply;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.comment.service.CommentService;
import spring.study.reply.service.ReplyService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.notification.service.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final CommentService commentService;
    private final ReplyService replyService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Long> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (!replyRequestDto.getReply().isBlank() || !replyRequestDto.getReply().isEmpty()) {
            int risk = forbiddenService.findWordList(Status.APPROVAL, replyRequestDto.getReply());

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3L);
                }

                return ResponseEntity.ok(-1L);
            }

            Comment comment = commentService.findById(replyRequestDto.getId());

            Member otherMember = comment.getMember();

            Reply result = replyService.save(replyRequestDto, member, comment);

            if (!member.getId().equals(otherMember.getId()))
                notificationService.createNotification(otherMember, member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다").addMember(otherMember);

            session.setAttribute("member", member);

            return ResponseEntity.ok(result.getId());
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
