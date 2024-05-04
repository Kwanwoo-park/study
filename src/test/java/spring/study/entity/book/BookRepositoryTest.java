package spring.study.entity.book;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.book.BookRequestDto;
import spring.study.entity.Book;
import spring.study.entity.Borrow;
import spring.study.entity.Condition;
import spring.study.service.BookService;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class BookRepositoryTest {
    @Autowired
    private BookService bookService;

    @Transactional
    @Test
    void save() {
        BookRequestDto bookSaveDto = new BookRequestDto();

        bookSaveDto.setBnum("123");
        bookSaveDto.setTitle("test");
        bookSaveDto.setAuthor("test");
        bookSaveDto.setPublisher("tester");
        bookSaveDto.setBsort("테스트");
        bookSaveDto.setSsort("test");
        bookSaveDto.setCond(Condition.양호);
        bookSaveDto.setBorw(Borrow.비치중);

        if (bookService.save(bookSaveDto).length() > 0) {
            findBookHash("test", 0, 5);
            findBook("test");
            bookReturn("123");
        }
    }

    void findBookHash(String title, Integer page, Integer size) {
        HashMap<String, Object> book = bookService.findBook(title, page, size);

        if (book != null) {
            System.out.println("# Success findBookHash() : " + book.toString());

            for (String s : book.keySet()) {
                System.out.println(book.get(s));
            }
        }
        else System.out.println("# Fail findBookHash() ~");
    }

    void findBook(String title) {
        Book book = bookService.findBookByTitle(title);

        if (book != null) System.out.println("# Success findBook() : " + book.toString());
        else System.out.println("# Fail findBook() ~");
    }

    void bookBorrow(String bnum) {
        int result = bookService.updateBookBorrow(bnum);

        if (result > 0) System.out.println("# Success bookBorrow() ~");
        else System.out.println("# Fail bookBorrow() ~");
    }

    void bookReturn(String bnum) {
        int result = bookService.updateBookReturn(bnum);

        if (result > 0) System.out.println("# Success bookReturn() ~");
        else System.out.println("# Fail bookReturn() ~");
    }

    @Transactional
    @Test
    void findAll() {
        Map<String, Object> result = bookService.findAll(0, 5);

        if (result != null) {
            System.out.println("# Success findAll() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else {
            System.out.println("# Fail findAll() ~");
        }
    }

    @Transactional
    @Test
    void findBorrowStatus() {
        HashMap<String, Object> result = bookService.findBorrow(Borrow.비치중, 0, 5);

        if (result != null) {
            System.out.println("# Success findBorrowStatus() : " + result.toString());

            for (String s: result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else {
            System.out.println("# Fail findBorrowStatus() ~");
        }
    }
}
