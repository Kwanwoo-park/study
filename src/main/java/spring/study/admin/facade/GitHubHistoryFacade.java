package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import spring.study.admin.dto.GitHubHistoryResponse;
import spring.study.admin.service.GitHubHistoryService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitHubHistoryFacade {
    private final GitHubHistoryService service;

    public ResponseEntity<GitHubHistoryResponse<GitHubHistoryResponse.Commit>> commits(
            int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.commits(page));
    }

    public ResponseEntity<GitHubHistoryResponse<GitHubHistoryResponse.Activity>> activity(
            String cursor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.activity(cursor));
    }
}
