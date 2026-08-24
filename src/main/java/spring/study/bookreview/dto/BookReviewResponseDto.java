package spring.study.bookreview.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.bookreview.entity.BookReview;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class BookReviewResponseDto {
    private Long id;
    private String reviewTitle;
    private String bookTitle;
    private String bookAuthor;
    private int rating;
    private LocalDate finishedDate;
    private String content;
    private String excerpt;
    private String writerName;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;

    public BookReviewResponseDto(BookReview review) {
        this.id = review.getId();
        this.reviewTitle = review.getReviewTitle();
        this.bookTitle = review.getBookTitle();
        this.bookAuthor = review.getBookAuthor();
        this.rating = review.getRating();
        this.finishedDate = review.getFinishedDate();
        this.content = review.getContent();
        this.excerpt = summarize(review.getContent());
        this.writerName = review.getAuthor().getName();
        this.registerTime = review.getRegisterTime();
        this.updateTime = review.getUpdateTime();
    }

    private String summarize(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "…";
    }
}
