package spring.study.dto.book;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.book.BookBorrow;

@Getter
@Setter
@NoArgsConstructor
public class BookBorrowRequestDto {
    private Long id;
    private String bnum;
    private String title;
    private Long mid;
    private String name;
    private String email;

    public BookBorrow toEntity() {
        return BookBorrow.builder()
                .bnum(bnum)
                .title(title)
                .mid(mid)
                .name(name)
                .email(email)
                .build();
    }
}
