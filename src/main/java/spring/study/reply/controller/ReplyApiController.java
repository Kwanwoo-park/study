package spring.study.reply.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.comment.dto.reply.ReplyResponseDto;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.reply.entity.Reply;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.comment.service.CommentService;
import spring.study.reply.service.ReplyService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/reply")
public class ReplyApiController {
    private final CommentService commentService;
    private final ReplyService replyService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Map<String, Long>> replyAPI(@RequestBody ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!replyRequestDto.getReply().isBlank() || !replyRequestDto.getReply().isEmpty()) {
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, replyRequestDto.getReply());

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

                Comment comment = commentService.findById(replyRequestDto.getId());

                Member otherMember = comment.getMember();

                Reply result = replyService.save(replyRequestDto, member, comment);
                map.put("result", result.getId());

                if (!member.getId().equals(otherMember.getId()))
                    notificationService.createNotification(otherMember, member.getName() + "님이 회원님의 댓글에 답글을 작성하였습니다", Group.REPLY).addMember(otherMember);

                session.setAttribute("member", member);

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getReply(@RequestParam() Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            List<ReplyResponseDto> reply = commentService.findById(id).getReply().stream().map(ReplyResponseDto::new).toList();

            map.put("result", 10L);
            map.put("list", reply);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
