package spring.study.todo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.todo.facade.WeekReviewFacade;

@Controller
@RequiredArgsConstructor
@RequestMapping("/todo")
public class TodoViewController {
    private final JwtManager jwtManager;
    private final WeekReviewFacade weekReviewFacade;

    @GetMapping({"", "/list"})
    public String list(Model model, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/todo";

        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());

        return "todo/list";
    }

    @GetMapping("/week")
    public String week(@RequestParam(defaultValue = "0") int weeksAgo, Model model,
                       HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?url=/todo/week";

        response.setHeader("Cache-Control", "no-store");
        return weekReviewFacade.week(member, model, weeksAgo);
    }
}
