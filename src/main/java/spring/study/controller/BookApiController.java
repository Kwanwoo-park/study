package spring.study.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.book.BookRequestDto;
import spring.study.service.BookBorrowService;
import spring.study.service.BookService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/book")
public class BookApiController {
    private final BookService bookService;
    private final BookBorrowService bookBorrowService;

    @PatchMapping("/borrow/action")
    public int bookBorrowAction(@RequestBody BookRequestDto bookUpdateDto) {
        return bookService.updateBookBorrow(bookUpdateDto.getBnum());
    }

    @PatchMapping("/return/action")
    public int bookReturnAction(@RequestBody BookRequestDto bookUpdateDto) {
        return bookService.updateBookReturn(bookUpdateDto.getBnum());
    }
}
