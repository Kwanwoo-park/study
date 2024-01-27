package spring.study.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(value = "/book/list", method = {RequestMethod.GET, RequestMethod.POST})
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
    public String bookFindClear() {
        book = null;

        return "redirect:/book/list";
    }

    @PostMapping("/book/list/action")
    public String bookFindAction(BookRequestDto bookRequestDto, Model model) throws Exception {
        try {
            book = bookService.findBook(bookRequestDto.getTitle());
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/book/list";
    }
}
