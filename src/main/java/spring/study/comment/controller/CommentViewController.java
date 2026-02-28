package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.comment.entity.Comment;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.board.service.BoardService;

import java.util.Comparator;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/comment")
public class CommentViewController {
    private final SessionManager sessionManager;
    private final BoardService boardService;

    @GetMapping("")
    public String getComments(Model model, @RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return "redirect:/member/login?error=true&exception=Not Found";

        model.addAttribute("comment", Map.of(
                "list", boardService.findById(id).getComment().stream().sorted(Comparator.comparingLong(Comment::getId).reversed()).toList()
        ));

        model.addAttribute("member", member.getEmail());

        return "comment/list";
    }
}
