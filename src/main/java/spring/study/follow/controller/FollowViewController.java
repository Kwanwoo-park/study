package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.common.service.SessionManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/follow")
public class FollowViewController {
    private final SessionManager sessionManager;
    private final MemberService memberService;

    @GetMapping("/follower")
    public String follower(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        Member follower = memberService.findMember(memberRequestDto.getEmail());

        model.addAttribute("follower", follower.getFollowing());
        model.addAttribute("follow", member.checkFollowing1(follower));
        model.addAttribute("email", member.getEmail());

        return "follow/follower";
    }

    @GetMapping("/following")
    public String following(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        Member following = memberService.findMember(memberRequestDto.getEmail());

        model.addAttribute("following", following.getFollower());
        model.addAttribute("email", member.getEmail());
        model.addAttribute("follow", member.checkFollowing2(following));

        return "follow/following";
    }
}
