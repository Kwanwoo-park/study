package spring.study.comment.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentFacade {
    private final CommentService commentService;
    private final BoardService boardService;
    private final NotificationService notificationService;
    private final ModerationService moderationService;

    public ResponseEntity<?> getCommentList(CommentRequestDto dto, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(dto.getComments(), member, request);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -10L,
                        "message", "댓글이 입력되지 않았습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        Board board = boardService.findById(dto.getId());
        Member otherMember = board.getMember();

        Comment comment = commentService.save(dto, member, board);

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember, member.getName() + "님이 게시물에 댓글을 작성하였습니다", Group.COMMENT).addMember(otherMember);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", comment.getId()
        ));
    }

    public ResponseEntity<?> updateComment(CommentRequestDto dto, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(dto.getComments(), member, request);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -10L,
                        "message", "댓글이 입력되지 않았습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", commentService.updateComments(dto.getId(), dto.getComments())
        ));
    }

    public ResponseEntity<?> deleteComment(Long id, CommentRequestDto dto, Member member, HttpServletRequest request) {
        Board board = boardService.findById(id);
        Comment comment = commentService.findById(dto.getId(), member, board);

        commentService.deleteById(dto.getId());

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", comment.getId()
        ));
    }
}
