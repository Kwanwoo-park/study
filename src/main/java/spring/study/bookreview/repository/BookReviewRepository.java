package spring.study.bookreview.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import spring.study.bookreview.entity.BookReview;
import spring.study.member.entity.Member;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    @Query("""
            select review from book_review review
            where :keyword = ''
               or lower(review.reviewTitle) like lower(concat('%', :keyword, '%'))
               or lower(review.bookTitle) like lower(concat('%', :keyword, '%'))
               or lower(review.bookAuthor) like lower(concat('%', :keyword, '%'))
            """)
    Page<BookReview> search(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("delete from book_review review where review.author = :author")
    void deleteByAuthor(@Param("author") Member author);
}
