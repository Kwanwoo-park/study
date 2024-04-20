package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.service.BookService;

import java.util.HashMap;

@RequiredArgsConstructor
@Controller
@RequestMapping("/book")
public class BookViewController {
    private final BookService bookService;

    private HashMap<String, Object> book;

    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "0") Integer page,
                       @RequestParam(required = false, defaultValue = "5") Integer size,
                       HttpSession session) throws Exception {
        book = (HashMap<String, Object>) session.getAttribute("book");

        try {
            if (book == null)
                model.addAttribute("book", bookService.findAll(page, size));
            else {
                model.addAttribute("book", book);
            }
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/book/list";
    }
}
