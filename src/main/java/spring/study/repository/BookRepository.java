package spring.study.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.Book;

public interface BookRepository extends JpaRepository<Book, String> {
    public Page<Book> findByTitle(String title, PageRequest pageRequest);
    public Book findByTitle(String title);
}
