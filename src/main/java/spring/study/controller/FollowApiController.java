package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.dto.search.SearchRequestDto;
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
    public Follow memberFollow(@RequestBody SearchRequestDto searchRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");
        Member search_member = memberService.findMember(searchRequestDto.getEmail());

        Follow follow = Follow.builder()
                .follower(member)
                .following(search_member)
                .build();

        member.addFollower(follow);
        search_member.addFollowing(follow);

        session.setAttribute("member", member);

        return followService.save(follow);
    }

    @DeleteMapping("/action")
    public void memberUnfollow(HttpSession session) {
        Member member = (Member) session.getAttribute("member");

        member.removeFollower(followService.findFollow(member));
        System.out.println(member.getFollower().size());

        followService.delete(member);

        session.setAttribute("member", member);
    }
}
