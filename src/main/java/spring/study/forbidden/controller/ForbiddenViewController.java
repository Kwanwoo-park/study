package spring.study.forbidden.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.common.service.SessionService;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/forbidden")
public class ForbiddenViewController {
    private final SessionService sessionService;

    @GetMapping("/list")
    public String forbiddenList(Model model, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "forbidden/list";
    }
}
