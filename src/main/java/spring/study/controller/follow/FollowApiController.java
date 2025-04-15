package spring.study.controller.follow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.follow.FollowService;
import spring.study.service.member.MemberService;
import spring.study.service.notification.NotificationService;

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
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = memberService.findMember(memberRequestDto.getEmail());

        if (followService.existFollow(member, search_member))
            return ResponseEntity.status(501).body(null);

        Follow follow = Follow.builder()
                .follower(member)
                .following(search_member)
                .build();

        member.addFollower(follow);
        search_member.addFollowing(follow);

        Notification notification = notificationService.createNotification(search_member, member.getName() + "님이 팔로우하기 시작하였습니다");
        notification.addMember(search_member);

        session.setAttribute("member", member);

        return ResponseEntity.ok(followService.save(follow));
    }

    @DeleteMapping("")
    public ResponseEntity<Member> memberUnfollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = memberService.findMember(memberRequestDto.getEmail());

        if (!followService.existFollow(member, search_member))
            return ResponseEntity.status(501).body(null);

        member.removeFollower(followService.findFollow(member, search_member));

        followService.delete(member, search_member);

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }
}
