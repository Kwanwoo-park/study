package spring.study.entity.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.service.BookService;

import java.util.Map;

@SpringBootTest
public class BookRepositoryTest {
    @Autowired
    private BookService bookService;

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
}
