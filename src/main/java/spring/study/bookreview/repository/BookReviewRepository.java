package spring.study.bookreview.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.bookreview.entity.BookReview;
import spring.study.member.entity.Member;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    @Transactional(readOnly = true)
    Page<BookReview> findByReviewTitleLikeIgnoreCaseOrBookTitleLikeIgnoreCaseOrBookAuthorLikeIgnoreCase(String reviewTitle, String bookTitle, String bookAuthor, Pageable pageable);

    @Transactional
    void deleteByAuthor(Member author);
}
