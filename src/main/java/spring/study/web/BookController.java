package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.book.BookRequestDto;
import spring.study.entity.book.Book;
import spring.study.service.BookService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class BookController {
    private final BookService bookService;

    private Book book;

    @GetMapping("/book/list")
    public String list(Model model) throws Exception {
        try {
            if (book == null)
                model.addAttribute("book", bookService.findAll());
            else {
                List<Book> list = new ArrayList<>();
                list.add(book);

                HashMap<String, Object> map = new HashMap<>();
                map.put("list", list);
                model.addAttribute("book", map);
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

    @GetMapping("/book/list/{title}/action")
    @ResponseBody
    public Book bookFindAction(@PathVariable String title) throws Exception {
        book = bookService.findBook(title);

        return book;
    }
}
