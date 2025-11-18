package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.member.dto.MemberRequestDto;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.follow.service.FollowService;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/follow")
public class FollowApiController {
    private final FollowService followService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("")
    public ResponseEntity<Follow> memberFollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = memberService.findMember(memberRequestDto.getEmail());

        if (member.getId().equals(search_member.getId()))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        if (followService.existFollow(member, search_member))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        Follow follow = followService.save(member, search_member);

        notificationService.createNotification(search_member, member.getName() + "님이 팔로우하기 시작하였습니다").addMember(search_member);

        session.setAttribute("member", member);

        return ResponseEntity.ok(follow);
    }

    @DeleteMapping("")
    public ResponseEntity<Member> memberUnfollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = memberService.findMember(memberRequestDto.getEmail());

        if (member.getId().equals(search_member.getId()))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        if (!followService.existFollow(member, search_member))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        followService.delete(followService.findFollow(member, search_member), member);

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }
}
