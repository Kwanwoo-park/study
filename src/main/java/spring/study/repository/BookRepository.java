package spring.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.Book;

public interface BookRepository extends JpaRepository<Book, String> {
    public Book findByTitle(String title);
}
