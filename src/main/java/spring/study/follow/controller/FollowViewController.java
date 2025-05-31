package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/follow")
public class FollowViewController {
    private final MemberService memberService;

    @GetMapping("/follower")
    public String follower(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        Member follower = memberService.findMember(memberRequestDto.getEmail());

        model.addAttribute("follower", follower.getFollowing());
        model.addAttribute("follow", member.checkFollowing1(follower));
        model.addAttribute("email", member.getEmail());

        return "follow/follower";
    }

    @GetMapping("/following")
    public String following(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        Member following = memberService.findMember(memberRequestDto.getEmail());

        model.addAttribute("following", following.getFollower());
        model.addAttribute("email", member.getEmail());
        model.addAttribute("follow", member.checkFollowing2(following));

        return "follow/following";
    }
}
