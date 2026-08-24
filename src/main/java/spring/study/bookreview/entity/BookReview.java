package spring.study.bookreview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

import java.time.LocalDate;

@Getter
@Entity(name = "book_review")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookReview extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_member_id", nullable = false)
    private Member author;

    @Column(name = "review_title", nullable = false, length = 200)
    private String reviewTitle;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "book_author", nullable = false, length = 100)
    private String bookAuthor;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "finished_date")
    private LocalDate finishedDate;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public BookReview(Long id, Member author, String reviewTitle, String bookTitle,
                      String bookAuthor, int rating, LocalDate finishedDate, String content) {
        this.id = id;
        this.author = author;
        this.reviewTitle = reviewTitle;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.rating = rating;
        this.finishedDate = finishedDate;
        this.content = content;
    }

    public void update(String reviewTitle, String bookTitle, String bookAuthor,
                       int rating, LocalDate finishedDate, String content) {
        this.reviewTitle = reviewTitle;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.rating = rating;
        this.finishedDate = finishedDate;
        this.content = content;
    }
}
