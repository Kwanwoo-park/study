package spring.study.controller.follow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.service.member.MemberService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/follow")
public class FollowViewController {
    private final MemberService memberService;

    @GetMapping("/follower")
    public String follower(Model model, MemberRequestDto memberRequestDto) {
        List<Follow> follower = memberService.findMember(memberRequestDto.getEmail()).getFollowing();

        model.addAttribute("follower", follower);

        return "follow/follower";
    }

    @GetMapping("/following")
    public String following(Model model, MemberRequestDto memberRequestDto) {
        List<Follow> following = memberService.findMember(memberRequestDto.getEmail()).getFollower();

        model.addAttribute("following", following);

        return "follow/following";
    }
}
