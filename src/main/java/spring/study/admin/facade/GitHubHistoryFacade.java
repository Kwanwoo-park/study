package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import spring.study.admin.service.GitHubHistoryService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitHubHistoryFacade {
    private final GitHubHistoryService service;

    public ResponseEntity<?> commits(int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.commits(page));
    }

    public ResponseEntity<?> activity(String cursor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.activity(cursor));
    }
}
