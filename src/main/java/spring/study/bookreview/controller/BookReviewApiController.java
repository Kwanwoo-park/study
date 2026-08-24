package spring.study.bookreview.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.dto.BookReviewResponseDto;
import spring.study.bookreview.service.BookReviewService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book-reviews")
public class BookReviewApiController {
    private final BookReviewService bookReviewService;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "") String keyword,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "9") int size,
                                  HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

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

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "review", bookReviewService.findById(id)
        ));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BookReviewRequestDto requestDto,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        if (bindingResult.hasErrors()) return validationFailure(bindingResult);
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        BookReviewResponseDto review = bookReviewService.create(requestDto, member);
        return ResponseEntity.ok(Map.of(
                "result", review.getId(),
                "review", review,
                "message", "독후감이 등록되었습니다"
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody BookReviewRequestDto requestDto,
                                    BindingResult bindingResult,
                                    HttpServletRequest request) {
        if (bindingResult.hasErrors()) return validationFailure(bindingResult);
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        BookReviewResponseDto review = bookReviewService.update(id, requestDto, member);
        return ResponseEntity.ok(Map.of(
                "result", review.getId(),
                "review", review,
                "message", "독후감이 수정되었습니다"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        bookReviewService.delete(id, member);
        return ResponseEntity.ok(Map.of(
                "result", id,
                "message", "독후감이 삭제되었습니다"
        ));
    }

    private ResponseEntity<?> validationFailure(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().isEmpty()
                ? "독후감 입력 내용을 확인해주세요"
                : bindingResult.getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "result", -10L,
                "message", message == null ? "독후감 입력 내용을 확인해주세요" : message
        ));
    }
}
