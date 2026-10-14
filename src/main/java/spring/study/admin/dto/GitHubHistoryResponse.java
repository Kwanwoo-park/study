package spring.study.admin.dto;

import java.time.Instant;
import java.util.List;

public record GitHubHistoryResponse<T>(String repository, String branch, List<T> entries, String nextCursor, Instant fetchedAt) {
}
