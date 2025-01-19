package spring.study.repository.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.book.BookBorrow;

public interface BookBorrowRepository extends JpaRepository<BookBorrow, String> {
    static final String book_return = "delete from book_borrow where bnum = :bnum " +
                                        "and title = :title";

    public BookBorrow findByTitle(String title);

    @Transactional
    @Modifying
    @Query(value = book_return, nativeQuery = true)
    public int bookReturn(@Param("bnum") String bnum, @Param("title") String title);
}
