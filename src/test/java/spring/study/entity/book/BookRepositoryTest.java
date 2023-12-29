package spring.study.entity.book;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.service.BookService;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class BookRepositoryTest {
    @Autowired
    private BookService bookService;

    @Transactional
    @Test
    void findAll() {
        Map<String, Object> result = bookService.findAll();

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
    void find() {
        String title = "미술가와 미술가";
        Book book = new Book();
        book.setAuthor("김이진");

        Book book1 = bookService.findBook(title);

        assertThat(book.getAuthor()).isEqualTo(book1.getAuthor());
    }
}
