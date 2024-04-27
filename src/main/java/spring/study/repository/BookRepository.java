package spring.study.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Book;

public interface BookRepository extends JpaRepository<Book, String> {
    static final String update_book = "update book set borw = 1 where bnum = :bnum";

    public Page<Book> findByTitle(String title, PageRequest pageRequest);
    public Book findByTitle(String title);

    @Transactional
    @Modifying
    @Query(value = update_book, nativeQuery = true)
    public int updateBorrowStatus(@Param("bnum") String bnum);
}
