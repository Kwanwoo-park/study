package spring.study.entity.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.book.BookRequestDto;

public interface BookRepository extends JpaRepository<Book, String> {
    static final String find_book = "select * from Book where title = :#{#bookRequestDto.title}";

    @Transactional
    @Query(value = find_book, nativeQuery = true)
    public int findBook(@Param("bookRequestDto")BookRequestDto bookRequestDto);
}
