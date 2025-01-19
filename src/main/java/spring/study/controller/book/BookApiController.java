package spring.study.controller.book;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.book.BookBorrowRequestDto;
import spring.study.service.book.BookBorrowService;
import spring.study.service.book.BookService;
import spring.study.service.member.MemberService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/book")
public class BookApiController {
    private final BookService bookService;
    private final BookBorrowService bookBorrowService;
    private final MemberService memberService;

    @PatchMapping("/borrow/action")
    public int bookBorrowAction(@RequestBody BookBorrowRequestDto bookUpdateDto) {
        return bookService.updateBookBorrow(bookUpdateDto.getBnum());
    }

    @PatchMapping("/return/action")
    public int bookReturnAction(@RequestBody BookBorrowRequestDto bookUpdateDto) {
        return bookService.updateBookReturn(bookUpdateDto.getBnum());
    }
}
