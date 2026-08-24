package spring.study.bookreview.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.bookreview.entity.BookReview;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookReviewRepositoryTest {
    @Autowired
    private BookReviewRepository bookReviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void bookReviewShouldPersistSearchAndDeleteByAuthor() {
        Member administrator = memberRepository.save(Member.builder()
                .email("book-admin@example.com")
                .pwd("password")
                .name("관리자")
                .role(Role.ADMIN)
                .phone("010-1234-5678")
                .birth("2000-01-01")
                .profile("profile.png")
                .build());
        BookReview review = bookReviewRepository.saveAndFlush(BookReview.builder()
                .author(administrator)
                .reviewTitle("설계 원칙을 다시 생각하다")
                .bookTitle("이펙티브 자바")
                .bookAuthor("조슈아 블로크")
                .rating(5)
                .finishedDate(LocalDate.of(2026, 8, 24))
                .content("좋은 API 설계에 관한 독후감입니다.")
                .build());

        assertThat(review.getId()).isNotNull();
        assertThat(bookReviewRepository.search("조슈아", PageRequest.of(0, 10)).getContent())
                .extracting(BookReview::getId)
                .containsExactly(review.getId());

        bookReviewRepository.deleteByAuthor(administrator);
        bookReviewRepository.flush();

        assertThat(bookReviewRepository.count()).isZero();
    }
}
