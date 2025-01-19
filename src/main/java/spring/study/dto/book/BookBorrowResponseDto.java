package spring.study.dto.book;

import lombok.Getter;
import spring.study.entity.book.BookBorrow;

@Getter
public class BookBorrowResponseDto {
    private Long id;
    private String bnum;
    private String title;
    private Long mid;
    private String name;
    private String email;

    public BookBorrowResponseDto(BookBorrow entity) {
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.mid = entity.getMid();
        this.name = entity.getName();
        this.email = entity.getEmail();
    }

    @Override
    public String toString() {
        return "BookBorrowResponseDto{" +
                "id=" + id +
                ", bnum='" + bnum + '\'' +
                ", title='" + title + '\'' +
                ", mid=" + mid +
                ", name='" + name + '\'' +
                '}';
    }
}
