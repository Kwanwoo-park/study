package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.follow.service.FollowService;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/follow")
public class FollowApiController {
    private final FollowService followService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Map<String, Long>> memberFollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
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

        if (memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        try {
            Member search_member = memberService.findMember(memberRequestDto.getEmail());

            if (member.getId().equals(search_member.getId())) {
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }

            if (followService.existFollow(member, search_member)) {
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
            }

            Follow follow = followService.save(member, search_member);
            map.put("result", follow.getId());

            notificationService.createNotification(search_member, member.getName() + "님이 팔로우하기 시작하였습니다", Group.FOLLOW).addMember(search_member);

            session.setAttribute("member", member);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());

            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<Map<String, Long>> memberUnfollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
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

        if (memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        try {
            Member search_member = memberService.findMember(memberRequestDto.getEmail());

            if (member.getId().equals(search_member.getId()))
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

            if (!followService.existFollow(member, search_member))
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

            followService.delete(followService.findFollow(member, search_member), member);

            map.put("result", 1L);

            session.setAttribute("member", member);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
