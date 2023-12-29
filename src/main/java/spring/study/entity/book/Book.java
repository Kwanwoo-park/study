package spring.study.entity.book;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import spring.study.entity.BasetimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String bnum;
    private String title;
    private String author;
    private String publisher;
    private String bsort;
    private String ssort;

    @Builder
    public Book(String bnum, String title, String author, String publisher, String bsort, String ssort) {
        this.bnum = bnum;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.bsort = bsort;
        this.ssort = ssort;
    }
}
