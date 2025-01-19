package spring.study.entity.book;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "book_borrow")
public class BookBorrow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String bnum;
    private String title;
    private Long mid;
    private String name;
    private String email;

    @Builder
    public BookBorrow(Long id, String bnum, String title, Long mid, String name, String email) {
        this.id = id;
        this.bnum = bnum;
        this.title = title;
        this.mid = mid;
        this.name = name;
        this.email = email;
    }
}
