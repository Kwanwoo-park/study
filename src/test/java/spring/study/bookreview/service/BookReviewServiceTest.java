package spring.study.bookreview.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.repository.BookReviewRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookReviewServiceTest {
    @Mock
    private BookReviewRepository bookReviewRepository;

    @InjectMocks
    private BookReviewService bookReviewService;

    @Test
    void regularMemberCannotCreateUpdateOrDeleteBookReview() {
        Member member = member(2L, Role.USER);

        assertForbidden(() -> bookReviewService.create(request(), member));
        assertForbidden(() -> bookReviewService.update(1L, request(), member));
        assertForbidden(() -> bookReviewService.delete(1L, member));

        verifyNoInteractions(bookReviewRepository);
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private BookReviewRequestDto request() {
        BookReviewRequestDto request = new BookReviewRequestDto();
        request.setReviewTitle("  좋은 삶에 대하여  ");
        request.setBookTitle("  이펙티브 자바  ");
        request.setBookAuthor("  조슈아 블로크  ");
        request.setRating(5);
        request.setFinishedDate(LocalDate.of(2026, 8, 24));
        request.setContent("  오래 남는 독후감 내용입니다.  ");
        return request;
    }

    private Member member(Long id, Role role) {
        return Member.builder()
                .id(id)
                .email(role.name().toLowerCase() + id + "@example.com")
                .pwd("password")
                .name(role == Role.ADMIN ? "관리자" : "회원")
                .role(role)
                .phone("010-0000-000" + id)
                .birth("2000-01-01")
                .profile("profile.png")
                .build();
    }
}
