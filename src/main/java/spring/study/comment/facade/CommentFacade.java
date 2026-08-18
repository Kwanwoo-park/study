package spring.study.comment.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.comment.dto.CommentListResponseDto;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentFacade {
    private final CommentService commentService;
    private final BoardService boardService;
    private final NotificationService notificationService;
    private final ModerationService moderationService;
    private final ReplyService replyService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;

    public ResponseEntity<?> saveComment(CommentRequestDto dto, Member member, HttpServletResponse response) {
        int risk = moderationService.validate(dto.getComments(), member, response);

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
        if (!visibilityAccessPolicy.canViewBoard(board, member)) {
            return forbiddenBoard();
        }
        Member otherMember = board.getMember();

        dto.setBoard(board);
        dto.setMember(member);

        Comment comment = commentService.save(dto.toEntity());

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember,
                    member.getName() + "님이 게시물에 댓글을 작성하였습니다",
                    Group.COMMENT,
                    board.getId().toString()
            ).addMember(otherMember);

        return ResponseEntity.ok(Map.of(
                "result", comment.getId()
        ));
    }

    public ResponseEntity<?> updateComment(CommentRequestDto dto, Member member, HttpServletResponse response) {
        Comment target = commentService.findById(dto.getId());
        if (!isOwnerOrAdmin(target, member)) {
            return forbiddenComment();
        }
        if (!visibilityAccessPolicy.canViewBoard(target.getBoard(), member)) {
            return forbiddenBoard();
        }

        int risk = moderationService.validate(dto.getComments(), member, response);

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
        Long commentId = dto != null && dto.getId() != null ? dto.getId() : id;
        Comment comment = commentService.findById(commentId);

        if (!isOwnerOrAdmin(comment, member)) {
            return forbiddenComment();
        }
        if (member.getRole() != Role.ADMIN
                && (comment.getBoard() == null || !comment.getBoard().getId().equals(id))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "게시글과 댓글 정보가 일치하지 않습니다"
            ));
        }

        replyService.deleteReplay(List.of(comment));
        commentService.deleteById(commentId);

        return ResponseEntity.ok(Map.of(
                "result", commentId
        ));
    }

    public ResponseEntity<?> getList(Long boardId, Member member, int cursor, int limit) {
        Board board = boardService.findById(boardId);
        if (!visibilityAccessPolicy.canViewBoard(board, member)) {
            return forbiddenBoard();
        }
        long totalCount = commentService.countComments(board);
        List<CommentListResponseDto> list = commentService.getComments(board, cursor, limit).stream()
                .map(CommentListResponseDto::new)
                .toList();
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "member", member.getEmail(),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "list", list
        ));
    }

    private boolean isOwnerOrAdmin(Comment comment, Member member) {
        return member.getRole() == Role.ADMIN
                || (comment.getMember() != null && comment.getMember().getId().equals(member.getId()));
    }

    private ResponseEntity<?> forbiddenComment() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -403L,
                "message", "본인이 작성한 댓글만 수정하거나 삭제할 수 있습니다"
        ));
    }

    private ResponseEntity<?> forbiddenBoard() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -403L,
                "message", "접근할 수 없는 게시글입니다"
        ));
    }
}
