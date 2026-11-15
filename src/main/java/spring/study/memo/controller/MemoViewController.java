package spring.study.memo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/memos")
public class MemoViewController {
    @GetMapping("")
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "memo/memos";
    }
}
