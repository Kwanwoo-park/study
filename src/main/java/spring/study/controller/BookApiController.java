package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.book.BookRequestDto;
import spring.study.entity.Borrow;
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
                                                  HttpSession session) {
        book = bookService.findBook(title, page, size);
        session.setAttribute("book", book);

        return book;
    }

    @GetMapping("/borrowStatus/{borrow}/action")
    public HashMap<String, Object> bookSelectBorrowAction(@PathVariable Borrow borrow,
                                                          @RequestParam(required = false, defaultValue = "0") Integer page,
                                                          @RequestParam(required = false, defaultValue = "5") Integer size,
                                                          HttpSession session) {
        book = bookService.findBorrow(borrow, page, size);
        session.setAttribute("book", book);

        return book;
    }

    @PatchMapping("/borrow/action")
    public int bookBorrowAction(@RequestBody BookRequestDto bookUpdateDto) {
        return bookService.updateBookBorrow(bookUpdateDto.getBnum());
    }

    @PatchMapping("/return/action")
    public int bookReturnAction(@RequestBody BookRequestDto bookUpdateDto) {
        return bookService.updateBookReturn(bookUpdateDto.getBnum());
    }
}
