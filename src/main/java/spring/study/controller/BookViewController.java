package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.Member;
import spring.study.service.BookService;

import java.util.HashMap;

@RequiredArgsConstructor
@Controller
@RequestMapping("/book")
public class BookViewController {
    private final BookService bookService;

    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "0") Integer page,
                       @RequestParam(required = false, defaultValue = "5") Integer size,
                       HttpSession session) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        HashMap<String, Object> book = (HashMap<String, Object>) session.getAttribute("book");
        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("role", member.getRole());

        if (book == null)
            model.addAttribute("book", bookService.findAll(page, size));
        else {
            model.addAttribute("book", book);
        }

        return "/book/list";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam() String title, Model model, HttpSession session) {
        if (session == null) {
            return "redirect:/member/login?error=true&exception=Session Expired";
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return "redirect:/member/login?error=true&exception=Login Please";
        }

        model.addAttribute("book", bookService.findBookByTitle(title));

        return "/book/detail";
    }
}
