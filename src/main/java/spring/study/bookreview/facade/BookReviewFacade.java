package spring.study.bookreview.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.service.BookReviewService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookReviewFacade {
    private final BookReviewService bookReviewService;

    public ResponseEntity<?> list(String keyword, int page, int size) {
        Page<BookReviewResponseDto> reviews = bookReviewService.findAll(keyword, page, size);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "reviews", reviews.getContent(),
                "page", reviews.getNumber(),
                "totalPages", reviews.getTotalPages(),
                "totalElements", reviews.getTotalElements(),
                "hasNext", reviews.hasNext()
        ));
    }

    public ResponseEntity<?> detail(Long id) {
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "review", bookReviewService.findById(id)
        ));
    }

    public ResponseEntity<?> create(BookReviewRequestDto requestDto, Member member) {
        BookReviewResponseDto review = bookReviewService.create(requestDto, member);

        return reviewResponse(review, "독후감이 등록되었습니다");
    }

    public ResponseEntity<?> update(Long id, BookReviewRequestDto requestDto, Member member) {
        BookReviewResponseDto review = bookReviewService.update(id, requestDto, member);

        return reviewResponse(review, "독후감이 수정되었습니다");
    }

    public ResponseEntity<?> delete(Long id, Member member) {
        bookReviewService.delete(id, member);

        return ResponseEntity.ok(Map.of(
                "result", id,
                "message", "독후감이 삭제되었습니다"
        ));
    }

    private ResponseEntity<?> reviewResponse(BookReviewResponseDto review, String message) {
        return ResponseEntity.ok(Map.of(
                "result", review.getId(),
                "review", review,
                "message", message
        ));
    }
}
