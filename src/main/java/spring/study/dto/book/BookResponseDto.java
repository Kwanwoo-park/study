package spring.study.dto.book;

import lombok.Getter;
import spring.study.entity.book.Book;

@Getter
public class BookResponseDto {
    private String bnum;
    private String title;
    private String author;
    private String publisher;
    private String bsort;
    private String ssort;

    public BookResponseDto(Book entity) {
        this.bnum = entity.getBnum();
        this.title = entity.getTitle();
        this.author = entity.getAuthor();
        this.publisher = entity.getPublisher();
        this.bsort = entity.getBsort();
        this.ssort = entity.getSsort();
    }

    @Override
    public String toString() {
        return "BookListDto [bnum=" + bnum + ", title=" + title + ", author=" + author
                + ", publisher=" + publisher + ", bsort=" + bsort + ", ssort=" + ssort + "]";
    }
}
