package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.MemberService;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final MemberService memberService;

    @GetMapping("/administrator")
    public String admin(HttpSession session){
        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "/admin/administrator";
    }

    @GetMapping("/member_check")
    public String member_check(Model model, HttpSession session){
        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findAll());

        return "/admin/member_check";
    }
}
