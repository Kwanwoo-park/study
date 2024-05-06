package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }


        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        return "/admin/administrator";
    }

    @GetMapping("/memberCheck")
    public String member_check(Model model, HttpSession session,
                               @RequestParam(required = false, defaultValue = "0") Integer page,
                               @RequestParam(required = false, defaultValue = "5") Integer size){
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }


        Member member = (Member) session.getAttribute("member");

        if (member.getRole() != Role.ADMIN) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Wrong Accept";
        }

        model.addAttribute("member", memberService.findAll(page, size));

        return "/admin/member_check";
    }
}
