package spring.study.todo.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@Controller
@RequiredArgsConstructor
@RequestMapping("/todo")
public class TodoViewController {
    private final JwtManager jwtManager;

    @GetMapping({"", "/list"})
    public String list(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/todo";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "todo/list";
    }
}
