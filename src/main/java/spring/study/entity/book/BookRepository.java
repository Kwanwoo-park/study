package spring.study.entity.book;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {
    public Book findByTitle(String title);
}
