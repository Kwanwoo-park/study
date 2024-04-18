package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.service.BookService;

import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/book")
public class BookApiController {
    private final BookService bookService;
    private HashMap<String, Object> book;

    @GetMapping("/list/{title}/action")
    public HashMap<String, Object> bookFindAction(@PathVariable String title,
                                                  @RequestParam(required = false, defaultValue = "0") Integer page,
                                                  @RequestParam(required = false, defaultValue = "5") Integer size,
                                                  HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();

        book = bookService.findBook(title, page, size);
        session.setAttribute("book", book);

        return book;
    }
}
