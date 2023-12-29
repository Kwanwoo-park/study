package spring.study.entity.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.book.BookRequestDto;

public interface BookRepository extends JpaRepository<Book, String> {
    public Book findByTitle(String title);
}
