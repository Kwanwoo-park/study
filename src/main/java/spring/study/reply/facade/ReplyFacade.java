package spring.study.reply.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.comment.dto.reply.ReplyResponseDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.member.entity.Member;
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
    private final NotificationService notificationService;
    private final ModerationService moderationService;

    public ResponseEntity<?> saveReply(ReplyRequestDto dto, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(dto.getReply(), member, request);

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
            notificationService.createNotification(otherMember,
                    member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다",
                    Group.REPLY,
                    comment.getBoard().getId().toString()
            ).addMember(otherMember);

        return ResponseEntity.ok(Map.of(
                "result", result.getId()
        ));
    }

    public ResponseEntity<?> getList(Long id, int cursor, int limit) {
        Comment comment = commentService.findById(id);
        long totalCount = replyService.countReplies(comment);
        List<ReplyResponseDto> list = replyService.getReplies(comment, cursor, limit)
                .stream().map(ReplyResponseDto::new).toList();
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "list", list
        ));
    }
}
