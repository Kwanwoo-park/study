package spring.study.memo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class MemoViewController {
    @GetMapping({"/memos", "/memos/"})
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "memo/memos";
    }

    @GetMapping({"/admin/memos", "/admin/memos/"})
    public String legacy() { return "redirect:/memos"; }
}
