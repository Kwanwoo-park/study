package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comment")
public class CommentApiController {
    private final CommentService commentService;
    private final BoardService boardService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Map<String, Long>> commentAction(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, commentRequestDto.getComments());

                if (risk != 0) {
                    if (risk == 3) {
                        notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
                        memberService.updateRole(member.getId(), Role.DENIED);

                        session.invalidate();

                        map.put("result", -3L);
                    } else
                        map.put("result", -1L);

                    return ResponseEntity.ok(map);
                }

                Board board = boardService.findById(commentRequestDto.getId());

                Member otherMember = board.getMember();

                Comment comment = commentService.save(commentRequestDto, member, board);
                map.put("result", comment.getId());

                if (!member.getId().equals(otherMember.getId())) {
                    notificationService.createNotification(otherMember, member.getName() + "님이 게시물에 댓글을 작성하였습니다", Group.COMMENT).addMember(otherMember);
                }

                session.setAttribute("member", member);

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());

                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @PatchMapping("/update")
    public ResponseEntity<Map<String, Long>> commentUpdate(@RequestBody CommentRequestDto commentRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!commentRequestDto.getComments().isBlank() || !commentRequestDto.getComments().isEmpty()) {
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, commentRequestDto.getComments());

                if (risk != 0) {
                    if (risk == 3) {
                        notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
                        memberService.updateRole(member.getId(), Role.DENIED);

                        session.invalidate();

                        map.put("result", -3L);
                    } else
                        map.put("result", -1L);

                    return ResponseEntity.ok(map);
                }

                map.put("result", commentService.updateComments(commentRequestDto.getId(), commentRequestDto.getComments()));

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());

                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Long>> commentDelete(@RequestParam Long id,
                                                 @RequestBody CommentRequestDto commentRequestDto,
                                                 HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        try {
            Board board = boardService.findById(id);
            Comment comment = commentService.findById(commentRequestDto.getId(), member, board);

            commentService.deleteById(commentRequestDto.getId());

            session.setAttribute("member", member);

            map.put("result", comment.getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());

            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
