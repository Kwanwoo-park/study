package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import spring.study.entity.book.Book;
import spring.study.service.BookService;

@RequiredArgsConstructor
@Controller
public class BookController {
    private final BookService bookService;

    private Book book;

    @GetMapping("/book/list")
    public String list(Model model) throws Exception {
        try {
            model.addAttribute("book", bookService.findAll());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/book/list";
    }
}
