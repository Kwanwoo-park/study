package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.service.FollowService;
import spring.study.service.MemberService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/follow")
public class FollowApiController {
    private final FollowService followService;
    private final MemberService memberService;

    @PostMapping("/action")
    public ResponseEntity<Follow> memberFollow(@RequestBody FollowRequestDto followRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = followRequestDto.getFollowing();

        if (followService.findFollow(member, search_member) != null)
            return ResponseEntity.status(501).body(null);

        Follow follow = Follow.builder()
                .follower(member)
                .following(search_member)
                .build();

        member.addFollower(follow);
        search_member.addFollowing(follow);

        session.setAttribute("member", member);

        return ResponseEntity.ok(followService.save(follow));
    }

    @DeleteMapping("/action")
    public ResponseEntity<Member> memberUnfollow(@RequestBody FollowRequestDto followRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Member search_member = followRequestDto.getFollowing();
        Follow follow = followService.findFollow(member, search_member);

        if (followService.findFollow(member, search_member) == null)
            return ResponseEntity.status(501).body(null);

        member.removeFollower(follow);

        followService.delete(member, search_member);

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }
}
