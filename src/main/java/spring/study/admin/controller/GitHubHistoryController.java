package spring.study.admin.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.GitHubHistoryResponse;
import spring.study.admin.service.GitHubHistoryService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GitHubHistoryController {
    private final GitHubHistoryService service;

    @GetMapping("/admin/github")
    public String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "admin/github";
    }

    @GetMapping("/api/admin/github/commits")
    @ResponseBody
    public ResponseEntity<GitHubHistoryResponse<GitHubHistoryResponse.Commit>> commits(
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.commits(page));
    }

    @GetMapping("/api/admin/github/activity")
    @ResponseBody
    public ResponseEntity<GitHubHistoryResponse<GitHubHistoryResponse.Activity>> activity(
            @RequestParam(defaultValue = "") String cursor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.activity(cursor));
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<?> unavailable(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", error.getReason() == null ? "내역 조회에 실패했습니다" : error.getReason()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<?> invalidPage() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("message", "올바른 페이지 번호를 입력해 주세요"));
    }
}
