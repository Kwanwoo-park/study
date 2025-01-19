package spring.study.dto.book;

import lombok.Getter;
import spring.study.entity.book.Book;
import spring.study.entity.book.Borrow;
import spring.study.entity.book.Condition;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class BookResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String bnum;
    private String title;
    private String author;
    private String publisher;
    private String bsort;
    private String ssort;
    private Condition cond;
    private Borrow borw;

    public BookResponseDto(Book entity) {
        this.id = entity.getId();
        this.bnum = entity.getBnum();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.publisher = entity.getPublisher();
        this.bsort = entity.getBsort();
        this.ssort = entity.getSsort();
        this.cond = entity.getCond();
        this.borw = entity.getBorw();
    }

    @Override
    public String toString() {
        return "BookListDto [bnum=" + bnum + ", title=" + title + ", author=" + author
                + ", publisher=" + publisher + ", bsort=" + bsort + ", ssort=" + ssort
                + ", cond=" + cond + ", borw=" + borw + "]";
    }
}
