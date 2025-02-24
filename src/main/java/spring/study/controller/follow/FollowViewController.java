package spring.study.controller.follow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.entity.member.Member;
import spring.study.service.member.MemberService;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/follow")
public class FollowViewController {
    private final MemberService memberService;
    private Member member;

    @GetMapping("/follower")
    public String follower(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        List<Follow> follower = memberService.findMember(memberRequestDto.getEmail()).getFollowing();

        model.addAttribute("follower", follower);

        return "follow/follower";
    }

    @GetMapping("/following")
    public String following(Model model, MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        List<Follow> following = memberService.findMember(memberRequestDto.getEmail()).getFollower();

        model.addAttribute("following", following);

        return "follow/following";
    }
}
