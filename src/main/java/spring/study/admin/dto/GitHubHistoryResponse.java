package spring.study.admin.dto;

import java.time.Instant;
import java.util.List;

public record GitHubHistoryResponse<T>(String repository, String branch, List<T> entries,
                                       String nextCursor, Instant fetchedAt) {
    public record Commit(String sha, String message, String author, String authoredAt,
                         String committedAt, String url) { }

    public record Activity(String id, String type, String actor, String pushedAt,
                           String ref, String before, String after, String url) { }
}
