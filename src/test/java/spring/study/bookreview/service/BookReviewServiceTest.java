package spring.study.bookreview.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.entity.BookReview;
import spring.study.bookreview.repository.BookReviewRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookReviewServiceTest {
    @Mock
    private BookReviewRepository bookReviewRepository;

    @InjectMocks
    private BookReviewService bookReviewService;

    @Test
    void administratorCanCreateBookReview() {
        Member administrator = member(1L, Role.ADMIN);
        BookReviewRequestDto request = request();
        when(bookReviewRepository.save(any(BookReview.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0), 11L));

        BookReviewResponseDto response = bookReviewService.create(request, administrator);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getReviewTitle()).isEqualTo("좋은 삶에 대하여");
        assertThat(response.getBookTitle()).isEqualTo("이펙티브 자바");
        assertThat(response.getRating()).isEqualTo(5);
        ArgumentCaptor<BookReview> captor = ArgumentCaptor.forClass(BookReview.class);
        verify(bookReviewRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(administrator);
    }

    @Test
    void regularMemberCannotCreateUpdateOrDeleteBookReview() {
        Member member = member(2L, Role.USER);

        assertForbidden(() -> bookReviewService.create(request(), member));
        assertForbidden(() -> bookReviewService.update(1L, request(), member));
        assertForbidden(() -> bookReviewService.delete(1L, member));

        verifyNoInteractions(bookReviewRepository);
    }

    @Test
    void administratorCanUpdateExistingBookReview() {
        Member administrator = member(1L, Role.ADMIN);
        BookReview existing = BookReview.builder()
                .id(9L)
                .author(administrator)
                .reviewTitle("기존 제목")
                .bookTitle("기존 책")
                .bookAuthor("기존 저자")
                .rating(3)
                .content("기존 내용")
                .build();
        when(bookReviewRepository.findById(9L)).thenReturn(Optional.of(existing));

        BookReviewResponseDto response = bookReviewService.update(9L, request(), administrator);

        assertThat(response.getReviewTitle()).isEqualTo("좋은 삶에 대하여");
        assertThat(response.getBookAuthor()).isEqualTo("조슈아 블로크");
        assertThat(response.getFinishedDate()).isEqualTo(LocalDate.of(2026, 8, 24));
        verify(bookReviewRepository, never()).save(any());
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

    private BookReview persisted(BookReview source, Long id) {
        return BookReview.builder()
                .id(id)
                .author(source.getAuthor())
                .reviewTitle(source.getReviewTitle())
                .bookTitle(source.getBookTitle())
                .bookAuthor(source.getBookAuthor())
                .rating(source.getRating())
                .finishedDate(source.getFinishedDate())
                .content(source.getContent())
                .build();
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
