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
import spring.study.memo.dto.MemoRequestDto;
import spring.study.memo.dto.MemoSettingsRequestDto;
import spring.study.memo.dto.MemoContentRequestDto;
import spring.study.memo.facade.MemoFacade;
import spring.study.member.entity.Member;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/memos", "/api/admin/memos"})
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class MemoApiController {
    private final MemoFacade memoFacade;

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
    public ResponseEntity<?> create(@RequestBody MemoRequestDto request, @AuthenticationPrincipal Member member,
                                    @RequestParam(defaultValue = "false") boolean draft) {
        return memoFacade.create(request, member, draft);
    }

    @PatchMapping("/{id}/draft")
    public ResponseEntity<?> saveDraft(@PathVariable Long id, @RequestBody MemoContentRequestDto request, @AuthenticationPrincipal Member member) {
        return memoFacade.saveDraft(id, request, member);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable Long id, @AuthenticationPrincipal Member member) {
        return memoFacade.status(id, member);
    }

    @PostMapping("/{id}/reload")
    public ResponseEntity<?> reload(@PathVariable Long id, @RequestParam Long version, @AuthenticationPrincipal Member member) {
        return memoFacade.reload(id, version, member);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MemoRequestDto request, @AuthenticationPrincipal Member member) {
        return memoFacade.update(id, request, member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestParam Long version, @AuthenticationPrincipal Member member) {
        return memoFacade.delete(id, version, member);
    }

    @PatchMapping("/{id}/settings")
    public ResponseEntity<?> updateSettings(@PathVariable Long id, @RequestBody MemoSettingsRequestDto request, @AuthenticationPrincipal Member member) {
        return memoFacade.updateSettings(id, request, member);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalidRequest() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("message", "메모 내용, 색상과 버전 정보를 확인해 주세요"));
    }
}
