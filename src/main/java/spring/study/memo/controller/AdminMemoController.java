package spring.study.memo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MissingServletRequestParameterException;
import spring.study.memo.dto.AdminMemoRequestDto;
import spring.study.memo.facade.AdminMemoFacade;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/memos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemoController {
    private final AdminMemoFacade memoFacade;

    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(CsrfToken token) {
        return memoFacade.csrf(token);
    }

    @GetMapping({"", "/"})
    public ResponseEntity<?> list(@AuthenticationPrincipal Member member, @RequestParam(defaultValue = "") String query,
                                  @RequestParam(defaultValue = "false") boolean pinnedOnly, @RequestParam(defaultValue = "0") int page) {
        return memoFacade.list(member, query, pinnedOnly, page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> find(@PathVariable Long id, @AuthenticationPrincipal Member member) {
        return memoFacade.find(id, member);
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> create(@RequestBody AdminMemoRequestDto request, @AuthenticationPrincipal Member member) {
        return memoFacade.create(request, member);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AdminMemoRequestDto request, @AuthenticationPrincipal Member member) {
        return memoFacade.update(id, request, member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestParam Long version, @AuthenticationPrincipal Member member) {
        return memoFacade.delete(id, version, member);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalidRequest() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("message", "메모 내용, 색상과 버전 정보를 확인해 주세요"));
    }
}
