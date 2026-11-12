package spring.study.reply.facade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.core.context.SecurityContextHolder;
import spring.study.common.service.ModerationService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.service.MemberService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.reply.dto.ReplyRequestDto;
import spring.study.reply.dto.ReplyResponseDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.reply.entity.Reply;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.reply.service.ReplyService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyFacade {
    private static final DateTimeFormatter SANCTION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final MemberTokenCacheService tokenCacheService;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieService jwtCookieService;
    private final CommentService commentService;
    private final ReplyService replyService;
    private final NotificationService notificationService;
    private final ModerationService moderationService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;

    @Transactional
    public ResponseEntity<?> saveReply(ReplyRequestDto dto, Member member, HttpServletResponse response) {
        int risk = validateModeratedContent(dto.getReply(), member, response);

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
        if (!visibilityAccessPolicy.canViewBoard(comment.getBoard(), member)) {
            return forbiddenBoard();
        }
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

    public ResponseEntity<?> getList(Long id, Member member, int cursor, int limit) {
        Comment comment = commentService.findById(id);
        if (!visibilityAccessPolicy.canViewBoard(comment.getBoard(), member)) {
            return forbiddenBoard();
        }
        long totalCount = replyService.countReplies(comment);
        List<ReplyResponseDto> list = replyService.getReplies(comment, cursor, limit)
                .stream().map(ReplyResponseDto::new).toList();
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "member", member.getEmail(),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "list", list
        ));
    }

    public ResponseEntity<?> updateReply(Long id, ReplyRequestDto dto, Member member, HttpServletResponse response) {
        Reply target = replyService.findById(id);
        if (!isOwnerOrAdmin(target, member)) return forbiddenReply();
        if (!visibilityAccessPolicy.canViewBoard(target.getComment().getBoard(), member)) return forbiddenBoard();

        int risk = validateModeratedContent(dto.getReply(), member, response);
        if (risk != 0) {
            return ResponseEntity.status(risk == -99 ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN).body(Map.of(
                    "result", risk == -99 ? -10L : -risk,
                    "message", risk == -99 ? "답글이 없습니다" : "금칙어를 사용하였습니다"
            ));
        }
        return ResponseEntity.ok(Map.of("result", replyService.update(id, dto.getReply())));
    }

    public ResponseEntity<?> deleteReply(Long id, Member member) {
        Reply target = replyService.findById(id);
        if (!isOwnerOrAdmin(target, member)) return forbiddenReply();
        replyService.deleteReply(id);
        return ResponseEntity.ok(Map.of("result", id));
    }

    private boolean isOwnerOrAdmin(Reply reply, Member member) {
        return member.getRole() == Role.ADMIN
                || (reply.getMember() != null && reply.getMember().getId().equals(member.getId()));
    }

    private ResponseEntity<?> forbiddenReply() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -403L,
                "message", "본인이 작성한 답글만 수정하거나 삭제할 수 있습니다"
        ));
    }

    private ResponseEntity<?> forbiddenBoard() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -403L,
                "message", "접근할 수 없는 게시글입니다"
        ));
    }

    private int validateModeratedContent(String checkStr, Member member, HttpServletResponse response) {
        int risk = evaluateModeration(checkStr, member);
        return risk == 3 ? handleModerationBlockRisk(member, response) : risk;
    }

    private int evaluateModeration(String content, Member member) {
        if (content == null || content.isBlank())
            return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, content);

        if (risk == 3 || risk == 0)
            return risk;

        return moderationService.evaluate(member, risk);
    }

    private int handleModerationBlockRisk(Member member, HttpServletResponse response) {
        if (member.getRole() == Role.ADMIN) {
            notificationService.createNotification(
                    member,
                    "경고일자: " + LocalDateTime.now().format(SANCTION_DATE_FORMAT) + " / 사유: 금칙어 사용 (관리자 계정 정지 제외)",
                    Group.ADMIN
            );
            moderationService.clear(member);

            return 1;
        }

        blockModeratedMember(member, response);
        return 3;
    }

    private void blockModeratedMember(Member member, HttpServletResponse response) {
        notificationService.createNotification(
                member,
                "금지일자: " + LocalDateTime.now().format(SANCTION_DATE_FORMAT) + " (영구) / 사유: 금칙어 사용",
                Group.ADMIN
        );

        notificationService.createNotification(
                memberService.findAdministrator(),
                member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                Group.ADMIN
        );

        memberService.updateRole(member.getId(), Role.DENIED);

        refreshTokenService.revoke(member.getId());
        tokenCacheService.delete(member.getId());
        jwtCookieService.clearAuthenticationCookies(response);
        SecurityContextHolder.clearContext();

        moderationService.clear(member);
    }

}
