package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.book.BookRequestDto;
import spring.study.dto.book.BookResponseDto;
import spring.study.entity.book.Book;
import spring.study.entity.book.BookRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BookService {
    private final BookRepository bookRepository;

    @Transactional
    public String save(BookRequestDto bookSaveDto) { return bookRepository.save(bookSaveDto.toEntity()).getBnum(); }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findAll() {
        HashMap<String, Object> book = new HashMap<>();

        List<Book> list = bookRepository.findAll();

        book.put("list", list.stream().map(BookResponseDto::new).collect(Collectors.toList()));

        return book;
    }

    public int findBook(BookRequestDto bookRequestDto) {return bookRepository.findBook(bookRequestDto); }
}
