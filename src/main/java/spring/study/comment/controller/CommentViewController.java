package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@Controller
@RequestMapping("/comment")
public class CommentViewController {
    private final SessionManager sessionManager;

    @GetMapping("")
    public String getComments(Model model, @RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found&url=/comment?id=" + id;

        model.addAttribute("member", member.getEmail());
        model.addAttribute("boardId", id);

        return "comment/list";
    }
}
