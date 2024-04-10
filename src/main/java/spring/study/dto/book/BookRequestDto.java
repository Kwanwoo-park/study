package spring.study.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Book;

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

    public Book toEntity() {
        return Book.builder()
                .bnum(bnum)
                .title(title)
                .author(author)
                .publisher(publisher)
                .bsort(bsort)
                .ssort(ssort)
                .build();
    }
}
