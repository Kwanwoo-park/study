package spring.study.repository.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.book.Book;
import spring.study.entity.book.Borrow;

public interface BookRepository extends JpaRepository<Book, String> {
    static final String book_borrow = "update book set borw = 1 where bnum = :bnum";

    static final String book_return = "update book set borw = 0 where bnum = :bnum";

    public Page<Book> findByTitle(String title, PageRequest pageRequest);
    public Book findByTitle(String title);
    public Page<Book> findByBorw(Borrow borrow, PageRequest pageRequest);

    @Transactional
    @Modifying
    @Query(value = book_return, nativeQuery = true)
    public int updateBookReturn(@Param("bnum") String bnum);

    @Transactional
    @Modifying
    @Query(value = book_borrow, nativeQuery = true)
    public int updateBookBorrow(@Param("bnum") String bnum);
}
