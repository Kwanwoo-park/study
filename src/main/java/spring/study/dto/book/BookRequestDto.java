package spring.study.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.book.Book;
import spring.study.entity.book.Borrow;
import spring.study.entity.book.Condition;

@Getter
@Setter
@NoArgsConstructor
public class BookRequestDto {
    private Long id;
    private String bnum;
    private String title;
    private String author;
    private String publisher;
    private String bsort;
    private String ssort;
    private Condition cond;
    private Borrow borw;

    public Book toEntity() {
        return Book.builder()
                .bnum(bnum)
                .title(title)
                .author(author)
                .publisher(publisher)
                .bsort(bsort)
                .ssort(ssort)
                .cond(Condition.양호)
                .borw(Borrow.비치중)
                .build();
    }
}
