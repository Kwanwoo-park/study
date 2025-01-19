package spring.study.service.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.book.BookBorrowRequestDto;
import spring.study.entity.book.BookBorrow;
import spring.study.repository.book.BookBorrowRepository;

@RequiredArgsConstructor
@Service
public class BookBorrowService {
    private final BookBorrowRepository bookBorrowRepository;

    @Transactional
    public String bookBorrow(BookBorrowRequestDto bookSaveDto) { return bookBorrowRepository.save(bookSaveDto.toEntity()).getBnum();}

    public int bookReturn(String bnum, String title) { return bookBorrowRepository.bookReturn(bnum, title); }

    public BookBorrow findTitle(String title) { return bookBorrowRepository.findByTitle(title); }
}
