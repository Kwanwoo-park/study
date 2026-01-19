package spring.study.comment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;
import spring.study.board.service.BoardService;
import spring.study.member.service.MemberService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/comment")
public class CommentViewController {
    private final BoardService boardService;
    private final MemberService memberService;

    @GetMapping("")
    public String getComments(Model model, @RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Session Invalid";
        }

        HashMap<String, Object> comment = new HashMap<>();
        List<Comment> list = boardService.findById(id).getComment();

        comment.put("list", list.stream().sorted(Comparator.comparingLong(Comment::getId).reversed()).toList());

        model.addAttribute("comment", comment);
        model.addAttribute("member", member.getEmail());

        return "comment/list";
    }
}
