package spring.study.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.Book;
import spring.study.service.BookService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/book")
public class BookController {
    private final BookService bookService;

    private HashMap<String, Object> book;

    @GetMapping("/list")
    public String list(Model model,
                       @RequestParam(required = false, defaultValue = "0") Integer page,
                       @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
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

    @GetMapping("/clear")
    @ResponseBody
    public void clear() {
        book = null;
    }

    @GetMapping("/list/{title}/action")
    @ResponseBody
    public HashMap<String, Object> bookFindAction(@PathVariable String title,
                                                  @RequestParam(required = false, defaultValue = "0") Integer page,
                                                  @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        book = bookService.findBook(title, page, size);

        return book;
    }
}
