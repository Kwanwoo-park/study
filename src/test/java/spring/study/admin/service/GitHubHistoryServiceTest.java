package spring.study.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GitHubHistoryServiceTest {
    private static final String SHA = "a".repeat(40);
    private static final String OTHER_SHA = "b".repeat(40);
    private static final String COMMITS = "https://api.github.com/repos/Kwanwoo-park/study/commits?per_page=20&sha=main&page=1";
    private static final String ACTIVITY = "https://api.github.com/repos/Kwanwoo-park/study/activity?per_page=20&ref=main&direction=desc";
    private final TestClock clock = new TestClock();
    private MockRestServiceServer server;
    private GitHubHistoryService service;

    @BeforeEach
    void setUp() {
        RestTemplate client = new RestTemplate();
        server = MockRestServiceServer.bindTo(client).build();
        service = new GitHubHistoryService(client, "Kwanwoo-park", "study", "main", "test-token", clock);
    }

    @Test
    void commitsUseReadOnlyRequestAndReturnMinimalDataWithRealPagination() {
        server.expect(requestTo(COMMITS)).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("X-GitHub-Api-Version", "2026-03-10"))
                .andRespond(withSuccess(commitJson(), MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.LINK, "<" + COMMITS.replace("page=1", "page=2") + ">; rel=\"next\""));
        var page = service.commits(1);
        assertThat(page.repository()).isEqualTo("Kwanwoo-park/study");
        assertThat(page.branch()).isEqualTo("main");
        assertThat(page.nextCursor()).isEqualTo("2");
        assertThat(page.entries()).hasSize(1);
        var entry = page.entries().get(0);
        assertThat(entry.message()).isEqualTo("fix: <script>alert(1)</script>");
        assertThat(entry.author()).isEqualTo("작성자");
        assertThat(entry.authoredAt()).isEqualTo("2026-09-10T01:00:00Z");
        assertThat(entry.committedAt()).isEqualTo("2026-09-11T01:00:00Z");
        assertThat(entry.url()).isEqualTo("https://github.com/Kwanwoo-park/study/commit/" + SHA);
        assertThat(entry.toString()).doesNotContain("private@example.test", "javascript:", "test-token");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "after"})
    void activityUsesPushTimeAndFollowsOpaqueGitHubPagination(String direction) {
        server.expect(requestTo(ACTIVITY)).andRespond(withSuccess(activityJson(), MediaType.APPLICATION_JSON)
                .header(HttpHeaders.LINK, "<" + ACTIVITY + "&" + direction + "=opaque%2B%2F%3D>; rel=\"next\""));
        server.expect(requestTo(ACTIVITY + "&" + direction + "=opaque%2B%2F%3D"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var page = service.activity("");
        var entry = page.entries().get(0);
        assertThat(entry.type()).isEqualTo("force");
        assertThat(entry.actor()).isEqualTo("admin-login");
        assertThat(entry.pushedAt()).isEqualTo("2026-09-12T04:00:00Z");
        assertThat(entry.ref()).isEqualTo("refs/heads/main");
        assertThat(entry.url()).endsWith("/compare/" + SHA + "..." + OTHER_SHA);
        assertThat(page.nextCursor()).isEqualTo(direction + ":opaque+/=");
        assertThat(service.activity(page.nextCursor()).entries()).isEmpty();
        server.verify();
    }

    @Test
    void olderActivityRepresentationAlsoKeepsThePushTime() {
        server.expect(requestTo(ACTIVITY)).andRespond(withSuccess("""
                [{"id":1,"activity_type":"pr_merge","actor":{"login":"merger"},
                  "timestamp":"2026-09-12T04:00:00Z","ref":"refs/heads/main"}]
                """, MediaType.APPLICATION_JSON));
        var entry = service.activity("").entries().get(0);
        assertThat(entry.type()).isEqualTo("pr_merge");
        assertThat(entry.actor()).isEqualTo("merger");
        assertThat(entry.pushedAt()).isEqualTo("2026-09-12T04:00:00Z");
        assertThat(entry.url()).isNull();
    }

    @Test
    void cachesForOneMinuteWithoutCallingGitHubAgain() {
        server.expect(requestTo(COMMITS)).andRespond(withSuccess(commitJson(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(COMMITS)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        var first = service.commits(1);
        clock.now = clock.now.plusSeconds(59);
        assertThat(service.commits(1)).isEqualTo(first);
        clock.now = clock.now.plusSeconds(2);
        assertThat(service.commits(1).entries()).isEmpty();
        server.verify();
    }

    @Test
    void rateLimitPausesUncachedRequestsAcrossBothTabs() {
        server.expect(requestTo(COMMITS)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .header("X-RateLimit-Remaining", "0").header("Retry-After", "120")
                .body("private upstream diagnostic with test-token"));
        server.expect(requestTo(ACTIVITY)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.commits(1)).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(error.getReason()).contains("조회 한도").doesNotContain("test-token", "private upstream");
        });
        clock.now = clock.now.plusSeconds(119);
        assertThatThrownBy(() -> service.activity("")).isInstanceOf(ResponseStatusException.class);
        clock.now = clock.now.plusSeconds(2);
        assertThat(service.activity("").entries()).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 429, 500})
    void upstreamErrorsNeverExposeTokensOrErrorBodies(int status) {
        server.expect(requestTo(COMMITS)).andRespond(withStatus(HttpStatus.valueOf(status)).body("secret diagnostic test-token"));
        assertThatThrownBy(() -> service.commits(1)).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(error.getReason()).doesNotContain("secret diagnostic", "test-token");
            assertThat(error.getCause()).isNull();
        });
        server.verify();
    }

    @Test
    void emptyRepositoryHasNoCommits() {
        server.expect(requestTo(COMMITS)).andRespond(withStatus(HttpStatus.CONFLICT));
        assertThat(service.commits(1).entries()).isEmpty();
        assertThat(service.commits(1).nextCursor()).isNull();
        server.verify();
    }

    @Test
    void malformedPayloadIsNotPresentedAsAnEmptyHistory() {
        server.expect(requestTo(COMMITS)).andRespond(withSuccess("{\"error\":true}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.commits(1)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void timeoutHasASafeRetryMessage() {
        server.expect(requestTo(COMMITS)).andRespond(withException(new IOException("private connection details")));
        assertThatThrownBy(() -> service.commits(1)).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getReason()).contains("연결하지 못했습니다").doesNotContain("private"));
    }

    @Test
    void rejectsInvalidPaginationBeforeAnyExternalRequest() {
        assertThatThrownBy(() -> service.commits(0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.commits(1001)).isInstanceOf(ResponseStatusException.class);
        for (String cursor : new String[]{"https://attacker.test", "before:", "before:x\n", "after:" + "a".repeat(513)}) {
            assertThatThrownBy(() -> service.activity(cursor)).isInstanceOf(ResponseStatusException.class);
        }
        server.verify();
    }

    @Test
    void doesNotFollowExternalPaginationLinks() {
        server.expect(requestTo(COMMITS)).andRespond(withSuccess(commitJson(), MediaType.APPLICATION_JSON)
                .header(HttpHeaders.LINK, "<https://attacker.test/repos/Kwanwoo-park/study/commits?page=2>; rel=\"next\""));
        assertThat(service.commits(1).nextCursor()).isNull();
        server.verify();
    }

    @Test
    void supportsPublicRepositoriesAndEncodesBranchAsOneQueryValue() {
        RestTemplate client = new RestTemplate();
        var publicServer = MockRestServiceServer.bindTo(client).build();
        var publicService = new GitHubHistoryService(client, "owner", "repo", "feature/a&b", "", clock);
        publicServer.expect(requestTo("https://api.github.com/repos/owner/repo/commits?per_page=20&sha=feature%2Fa%26b&page=1"))
                .andExpect(headerDoesNotExist("Authorization")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(publicService.commits(1).entries()).isEmpty();
        publicServer.verify();
    }

    private String commitJson() {
        return """
                [{"sha":"%s","html_url":"javascript:alert(1)",
                "commit":{"message":"fix: <script>alert(1)</script>",
                "author":{"name":"작성자","email":"private@example.test","date":"2026-09-10T01:00:00Z"},
                "committer":{"date":"2026-09-11T01:00:00Z"}}}]
                """.formatted(SHA);
    }

    private String activityJson() {
        return """
                [{"id":42,"before":"%s","after":"%s","ref":"refs/heads/main",
                "pushed_at":"2026-09-12T04:00:00Z","push_type":"force","pusher":{"login":"admin-login"}}]
                """.formatted(SHA, OTHER_SHA);
    }

    private static class TestClock extends Clock {
        Instant now = Instant.parse("2026-09-12T05:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
