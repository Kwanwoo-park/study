package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.service.FollowService;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class FollowController {
    private final FollowService followService;

    @GetMapping("/follow/follower")
    public String follower(Model model, MemberRequestDto memberRequestDto) {
        List<Follow> follower = followService.findFollowing(memberRequestDto.getId());

        model.addAttribute("follower", follower);

        return "/follow/follower";
    }

    @GetMapping("/follow/following")
    public String following(Model model, MemberRequestDto memberRequestDto) {
        List<Follow> following = followService.findFollower(memberRequestDto.getId());

        model.addAttribute("following", following);

        return "/follow/following";
    }
}
