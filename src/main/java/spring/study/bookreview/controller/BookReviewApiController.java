package spring.study.bookreview.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import spring.study.bookreview.dto.BookReviewRequestDto;
import spring.study.bookreview.facade.BookReviewFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book-reviews")
public class BookReviewApiController {
    private final BookReviewFacade bookReviewFacade;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "") String keyword,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "9") int size, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return bookReviewFacade.list(keyword, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return bookReviewFacade.detail(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BookReviewRequestDto requestDto,
                                    BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "독후감 입력 내용을 확인해주세요");
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return bookReviewFacade.create(requestDto, member);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody BookReviewRequestDto requestDto,
                                    BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) return commonFacade.validationFailure(bindingResult, "독후감 입력 내용을 확인해주세요");
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return bookReviewFacade.update(id, requestDto, member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return bookReviewFacade.delete(id, member);
    }
}
