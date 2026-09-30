package spring.study.admin.controller;

import org.springframework.web.bind.annotation.*;
import spring.study.admin.facade.GitHubHistoryFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/github")
@PreAuthorize("hasRole('ADMIN')")
public class GitHubHistoryController {
    private final GitHubHistoryFacade facade;

    @GetMapping("/commits")
    public ResponseEntity<?> commits(@RequestParam(defaultValue = "1") int page) {
        return facade.commits(page);
    }

    @GetMapping("/activity")
    public ResponseEntity<?> activity(@RequestParam(defaultValue = "") String cursor) {
        return facade.activity(cursor);
    }
}
