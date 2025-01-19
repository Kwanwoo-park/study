package spring.study.entity.book;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity(name = "book")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bnum;
    private String title;
    private String author;
    private String publisher;
    private String bsort;
    private String ssort;
    private Condition cond;
    private Borrow borw;

    @Builder
    public Book(Long id, String bnum, String title, String author, String publisher,
                String bsort, String ssort, Condition cond, Borrow borw) {
        this.id = id;
        this.bnum = bnum;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.bsort = bsort;
        this.ssort = ssort;
        this.cond = cond;
        this.borw = borw;
    }
}
