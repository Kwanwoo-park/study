package spring.study.reply.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.comment.dto.reply.ReplyResponseDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.entity.Reply;
import spring.study.reply.service.ReplyService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyFacade {
    private final CommentService commentService;
    private final ReplyService replyService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public ResponseEntity<?> saveReply(ReplyRequestDto dto, Member member, HttpServletRequest request) {
        int risk = validateReply(dto.getReply(), member, request);

        if (risk != 0) {
            if (risk == -99)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", risk,
                        "message", "답글이 없습니다"
                ));

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        Comment comment = commentService.findById(dto.getId());
        Member otherMember = comment.getMember();
        Reply result = replyService.save(dto, member, comment);

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember, member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다", Group.REPLY).addMember(otherMember);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", result.getId()
        ));
    }

    public ResponseEntity<?> getList(Long id) {
        List<ReplyResponseDto> list = commentService.findById(id).getReply()
                .stream().map(ReplyResponseDto::new).toList();

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", list
        ));
    }

    private int validateReply(String reply, Member member, HttpServletRequest request) {
        if (reply == null || reply.isBlank()) return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, reply);

        if (risk == 3) {
            notificationService.createNotification(
                    memberService.findAdministrator(),
                    member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                    Group.ADMIN
            );

            memberService.updateRole(member.getId(), Role.DENIED);

            request.getSession(false).invalidate();
        }

        return risk;
    }
}
