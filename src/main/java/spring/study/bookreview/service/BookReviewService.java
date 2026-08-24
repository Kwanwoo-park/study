package spring.study.bookreview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.entity.BookReview;
import spring.study.bookreview.repository.BookReviewRepository;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

@Service
@RequiredArgsConstructor
public class BookReviewService {
    private static final int MAX_PAGE_SIZE = 30;

    private final BookReviewRepository bookReviewRepository;

    @Transactional(readOnly = true)
    public Page<BookReviewResponseDto> findAll(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("registerTime"), Sort.Order.desc("id"))
        );
        return bookReviewRepository.search(normalizedKeyword, pageable).map(BookReviewResponseDto::new);
    }

    @Transactional(readOnly = true)
    public BookReviewResponseDto findById(Long id) {
        return new BookReviewResponseDto(findEntity(id));
    }

    @Transactional
    public BookReviewResponseDto create(BookReviewRequestDto requestDto, Member member) {
        requireAdministrator(member);
        BookReview review = BookReview.builder()
                .author(member)
                .reviewTitle(requestDto.getReviewTitle().trim())
                .bookTitle(requestDto.getBookTitle().trim())
                .bookAuthor(requestDto.getBookAuthor().trim())
                .rating(requestDto.getRating())
                .finishedDate(requestDto.getFinishedDate())
                .content(requestDto.getContent().trim())
                .build();
        return new BookReviewResponseDto(bookReviewRepository.save(review));
    }

    @Transactional
    public BookReviewResponseDto update(Long id, BookReviewRequestDto requestDto, Member member) {
        requireAdministrator(member);
        BookReview review = findEntity(id);
        review.update(
                requestDto.getReviewTitle().trim(),
                requestDto.getBookTitle().trim(),
                requestDto.getBookAuthor().trim(),
                requestDto.getRating(),
                requestDto.getFinishedDate(),
                requestDto.getContent().trim()
        );
        return new BookReviewResponseDto(review);
    }

    @Transactional
    public void delete(Long id, Member member) {
        requireAdministrator(member);
        bookReviewRepository.delete(findEntity(id));
    }

    @Transactional
    public void deleteByAuthor(Member author) {
        bookReviewRepository.deleteByAuthor(author);
    }

    public void requireAdministrator(Member member) {
        if (member == null || member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 독후감을 관리할 수 있습니다");
        }
    }

    private BookReview findEntity(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("존재하지 않는 독후감입니다");
        }
        return bookReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 독후감입니다"));
    }
}
