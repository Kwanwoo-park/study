package spring.study.service.book;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.book.BookRequestDto;
import spring.study.dto.book.BookResponseDto;
import spring.study.entity.book.Book;
import spring.study.entity.book.Borrow;
import spring.study.repository.book.BookRepository;
import java.util.HashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BookService {
    private final BookRepository bookRepository;

    @Transactional
    public String save(BookRequestDto bookSaveDto) { return bookRepository.save(bookSaveDto.toEntity()).getBnum(); }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> book = new HashMap<>();

        Page<Book> list = bookRepository.findAll(PageRequest.of(page, size, Sort.by("bnum").ascending()));

        book.put("list", list.stream().map(BookResponseDto::new).collect(Collectors.toList()));
        book.put("paging", list.getPageable());
        book.put("totalCnt", list.getTotalElements());
        book.put("totalPage", list.getTotalPages());

        return book;
    }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findBook(String title, Integer page, Integer size) {
        HashMap<String, Object> book = new HashMap<>();

        Page<Book> list = bookRepository.findByTitle(title, PageRequest.of(page, size, Sort.by("bnum").ascending()));
        book.put("list", list.stream().map(BookResponseDto::new).collect(Collectors.toList()));
        book.put("paging", list.getPageable());
        book.put("totalCnt", list.getTotalElements());
        book.put("totalPage", list.getTotalPages());

        return book;
    }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findBorrow(Borrow borrow, Integer page, Integer size) {
        HashMap<String, Object> book = new HashMap<>();

        Page<Book> list = bookRepository.findByBorw(borrow, PageRequest.of(page, size, Sort.by("bnum").ascending()));
        book.put("list", list.stream().map(BookResponseDto::new).collect(Collectors.toList()));
        book.put("paging", list.getPageable());
        book.put("totalCnt", list.getTotalElements());
        book.put("totalPage", list.getTotalPages());

        return book;
    }

    public Book findBookByTitle(String title) {
        return bookRepository.findByTitle(title);
    }

    public int updateBookReturn(String bnum) { return bookRepository.updateBookReturn(bnum); }

    public int updateBookBorrow(String bnum) { return bookRepository.updateBookBorrow(bnum); }
}
