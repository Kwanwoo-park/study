package spring.study.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Book;
import spring.study.entity.Borrow;
import spring.study.entity.Condition;

@Getter
@Setter
@NoArgsConstructor
public class BookRequestDto {
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
                .cond(Condition.FINE)
                .borw(Borrow.UNBORROW)
                .build();
    }
}
